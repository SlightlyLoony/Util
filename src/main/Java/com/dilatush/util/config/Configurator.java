package com.dilatush.util.config;

/**
 * Implemented by classes that configure a configuration class.
 */
public interface Configurator {


    /**
     * Configure the given instance of {@link AConfig}.
     *
     * @param _config The instance of {@link AConfig} to configure.
     */
    void config( final AConfig _config );
}
