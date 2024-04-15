package io.bdrc.lucene.bo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.AccessControlException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonHelpers {
    static final Logger logger = LoggerFactory.getLogger(CommonHelpers.class);
    public static final String baseDir = "src/main/resources/";

    public static InputStream getResourceOrFile(final String baseName) {
        InputStream stream = null;
        stream = CommonHelpers.class.getClassLoader().getResourceAsStream("/" + baseName);
        if (stream != null) {
            logger.info("found resource /{} through regular classloader", baseName);
            return stream;
        }
        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("/" + baseName);
        if (stream != null) {
            logger.info("found resource /{} through thread context classloader", baseName);
            return stream;
        }
        stream = CommonHelpers.class.getResourceAsStream(baseName);
        if (stream != null) {
            logger.info("found resource /{} through direct classloader", baseName);
            return stream;
        }
        final String fileBaseName = baseDir + baseName;
        try {
            stream = new FileInputStream(fileBaseName);
            logger.info("found file {}", fileBaseName);
            return stream;
        } catch (FileNotFoundException | AccessControlException e) {
            logger.info("could not find file {}", fileBaseName);
            return null;
        }
    }

}