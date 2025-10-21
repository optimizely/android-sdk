
class DefaultCmabClient @JvmOverloads constructor(
    httpClient: OptimizelyHttpClient? = null,
    config: CmabClientConfig? = CmabClientConfig.withNoRetry()
) : CmabClient {
    private val httpClient: OptimizelyHttpClient
    private val retryConfig: RetryConfig?

    // Primary constructor - all others delegate to this
    // Default constructor (no retry, default HTTP client)
    init {
        retryConfig = if (config != null) config.getRetryConfig() else null
        this.httpClient = if (httpClient != null) httpClient else createDefaultHttpClient()
    }

    // Constructor with HTTP client only (no retry)
    constructor(httpClient: OptimizelyHttpClient?) : this(
        httpClient,
        CmabClientConfig.withNoRetry()
    )

    // Constructor with just retry config (uses default HTTP client)
    constructor(config: CmabClientConfig?) : this(null, config)

    // Extract HTTP client creation logic
    private fun createDefaultHttpClient(): OptimizelyHttpClient {
        val timeoutMs =
            if (retryConfig != null) retryConfig.getMaxTimeoutMs() else DEFAULT_TIMEOUT_MS
        return OptimizelyHttpClient.builder().setTimeoutMillis(timeoutMs).build()
    }

    fun fetchDecision(
        ruleId: String?,
        userId: String?,
        attributes: Map<String?, Any?>?,
        cmabUuid: String?
    ): String {
        // Implementation will use this.httpClient and this.retryConfig
        val url = String.format(CMAB_PREDICTION_ENDPOINT, ruleId)
        val requestBody: String =
            CmabClientHelper.buildRequestJson(userId, ruleId, attributes, cmabUuid)

        // Use retry logic if configured, otherwise single request
        return if (retryConfig != null && retryConfig.getMaxRetries() > 0) {
            doFetchWithRetry(url, requestBody, retryConfig.getMaxRetries())
        } else {
            doFetch(url, requestBody)
        }
    }

    private fun doFetch(url: String, requestBody: String): String {
        val request = HttpPost(url)
        try {
            request.setEntity(StringEntity(requestBody))
        } catch (e: UnsupportedEncodingException) {
            val errorMessage: String =
                java.lang.String.format(CmabClientHelper.CMAB_FETCH_FAILED, e.message)
            logger.error(errorMessage)
            throw CmabFetchException(errorMessage)
        }
        request.setHeader("content-type", "application/json")
        var response: CloseableHttpResponse? = null
        return try {
            response = httpClient.execute(request)
            if (!CmabClientHelper.isSuccessStatusCode(response.getStatusLine().getStatusCode())) {
                val statusLine: StatusLine = response.getStatusLine()
                val errorMessage: String = java.lang.String.format(
                    CmabClientHelper.CMAB_FETCH_FAILED,
                    statusLine.getReasonPhrase()
                )
                logger.error(errorMessage)
                throw CmabFetchException(errorMessage)
            }
            val responseBody: String
            try {
                responseBody = EntityUtils.toString(response.getEntity())
                if (!CmabClientHelper.validateResponse(responseBody)) {
                    logger.error(CmabClientHelper.INVALID_CMAB_FETCH_RESPONSE)
                    throw CmabInvalidResponseException(CmabClientHelper.INVALID_CMAB_FETCH_RESPONSE)
                }
                CmabClientHelper.parseVariationId(responseBody)
            } catch (e: IOException) {
                logger.error(CmabClientHelper.CMAB_FETCH_FAILED)
                throw CmabInvalidResponseException(CmabClientHelper.INVALID_CMAB_FETCH_RESPONSE)
            } catch (e: ParseException) {
                logger.error(CmabClientHelper.CMAB_FETCH_FAILED)
                throw CmabInvalidResponseException(CmabClientHelper.INVALID_CMAB_FETCH_RESPONSE)
            }
        } catch (e: IOException) {
            val errorMessage: String =
                java.lang.String.format(CmabClientHelper.CMAB_FETCH_FAILED, e.message)
            logger.error(errorMessage)
            throw CmabFetchException(errorMessage)
        } finally {
            closeHttpResponse(response)
        }
    }

    private fun doFetchWithRetry(url: String, requestBody: String, maxRetries: Int): String {
        var backoff: Double = retryConfig.getBackoffBaseMs()
        var lastException: Exception? = null
        for (attempt in 0..maxRetries) {
            try {
                return doFetch(url, requestBody)
            } catch (e: CmabFetchException) {
                lastException = e

                // If this is the last attempt, don't wait - just break and throw
                if (attempt >= maxRetries) {
                    break
                }

                // Log retry attempt
                logger.info(
                    "Retrying CMAB request (attempt: {}) after {} ms...",
                    attempt + 1, backoff.toInt()
                )
                try {
                    Thread.sleep(backoff.toLong())
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    val errorMessage: String = java.lang.String.format(
                        CmabClientHelper.CMAB_FETCH_FAILED,
                        "Request interrupted during retry"
                    )
                    logger.error(errorMessage)
                    throw CmabFetchException(errorMessage, ie)
                }

                // Calculate next backoff using exponential backoff with multiplier
                backoff = Math.min(
                    backoff * Math.pow(
                        retryConfig.getBackoffMultiplier(),
                        (attempt + 1).toDouble()
                    ),
                    retryConfig.getMaxTimeoutMs()
                )
            } catch (e: CmabInvalidResponseException) {
                lastException = e
                if (attempt >= maxRetries) {
                    break
                }
                logger.info(
                    "Retrying CMAB request (attempt: {}) after {} ms...",
                    attempt + 1, backoff.toInt()
                )
                try {
                    Thread.sleep(backoff.toLong())
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    val errorMessage: String = java.lang.String.format(
                        CmabClientHelper.CMAB_FETCH_FAILED,
                        "Request interrupted during retry"
                    )
                    logger.error(errorMessage)
                    throw CmabFetchException(errorMessage, ie)
                }
                backoff = Math.min(
                    backoff * Math.pow(
                        retryConfig.getBackoffMultiplier(),
                        (attempt + 1).toDouble()
                    ),
                    retryConfig.getMaxTimeoutMs()
                )
            }
        }

        // If we get here, all retries were exhausted
        val errorMessage: String = java.lang.String.format(
            CmabClientHelper.CMAB_FETCH_FAILED,
            "Exhausted all retries for CMAB request"
        )
        logger.error(errorMessage)
        throw CmabFetchException(errorMessage, lastException)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultCmabClient::class.java)
        private const val DEFAULT_TIMEOUT_MS = 10000
        private const val CMAB_PREDICTION_ENDPOINT =
            "https://prediction.cmab.optimizely.com/predict/%s"

        private fun closeHttpResponse(response: CloseableHttpResponse?) {
            if (response != null) {
                try {
                    response.close()
                } catch (e: IOException) {
                    logger.warn(e.localizedMessage)
                }
            }
        }
    }
}
