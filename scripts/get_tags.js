#!/usr/bin/env node

const optimizely = require("@optimizely/optimizely-sdk")

// Determines which tags to exclude from the test run depending on the SDK we are testing
const SDK = process.env.SDK
const UPSTREAM_REPO = process.env.UPSTREAM_REPO
const SDK_KEY = process.env.FEATURE_DATAFILE_SDK_KEY

const upstreamRepoTags = {
  "@DATAFILE_MANAGER": []
}

let optimizelyClient = optimizely.createInstance({
  sdkKey: SDK_KEY,
  datafileOptions: {
    autoUpdate: false,
  }
})

optimizelyClient.onReady().then(() => {
  const allTags = Object.keys(optimizelyClient.projectConfigManager.getConfig().featureKeyMap)
  const enabledTags = optimizelyClient.getEnabledFeatures('tests_user', {'sdk': SDK})
  const disabledTags = allTags.filter(tag => enabledTags.indexOf(tag) === -1)
  const tagExpression = disabledTags.map((tag) => {
    // if UPSTREAM_REPO is set, it means we should be running the tests for that repo instead of the public one
    if (UPSTREAM_REPO.indexOf("-sdk-dev") >= 0 && upstreamRepoTags[tag]) {
      // check if we should be running the tests with this tag in the upstream repo
      if (upstreamRepoTags[tag].indexOf(SDK) < 0) {
        return `not @${tag}`
      }
    }

    return `not @${tag}`
  }).join(' and ')
  // Spit it out to the bash script calling this
  console.log(tagExpression)
})
