package com.optimizely.ab.bucketing;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;

/**
 * Interface for persisting variation activations for an experiment
 *
 * Persistent bucketing is useful when the variation for a user should not ever change, even if
 * traffic allocation changes.
 *
 * Create an implementation of this interface and pass it to
 * {@link com.optimizely.ab.Optimizely.Builder#withPersistentBucketer(PersistentBucketer)} to inject
 * it into the returned {@link Optimizely} instance.  The default Optimizely bucketer will
 * check the provided persistent bucketer for a persisted activation while bucketing.  If the a variation
 * is returned that variation is used.  Return null for {@link this#restoreActivation(ProjectConfig, String, Experiment)}
 * to tell the bucketer to bucket normally.
 *
 * Likewise, when the initial bucketing occurs the default Optimizely {@link Bucketer} will provide
 * the {@link ProjectConfig projectConfig}, userId, {@link Experiment}, and
 * {@link Variation} to {@link this#saveActivation(String, Experiment, Variation)}.
 * These parameters should provide enough data to persist the activation and restore an instance of
 * {@link Variation}
 *
 * If providing a custom {@link Bucketer} be sure to make it's bucketing methods respect the presence
 * of a {@link PersistentBucketer} if desired.  {@link PersistentBucketer} instances should be composed
 * inside of {@link Bucketer} instances.
 *
 *
 */
public interface PersistentBucketer {

    /**
     * Called after bucketing occurs, providing a chance to persist data about the activation
     * @param userId a String representing a user ID
     * @param experiment an {@link Experiment} instance
     * @param variation a {@link Variation} id
     * @return true if saved otherwise false
     */
    boolean saveActivation(String userId, Experiment experiment, Variation variation);

    /**
     * Called before bucketing occurs, providing a chance to restore a previously bucketed {@link Variation}
     * @param projectConfig an  instance of {@link ProjectConfig}
     * @param userId a String representing a user ID
     * @param experiment an {@link Experiment} instance
     * @return an instance of {@link Variation}
     *          The {@link ProjectConfig} instance can be used to look up the {@link Variation} instance
     *          for the project via the userID string and the {@link Experiment} instance which has the experiment ID.
     */
    Variation restoreActivation(ProjectConfig projectConfig, String userId, Experiment experiment);
}
