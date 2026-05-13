package com.soulmate.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backend")
public class BackendProperties {

    private final Security security = new Security();
    private final Gemini gemini = new Gemini();
    private final Cloudinary cloudinary = new Cloudinary();

    public Security getSecurity() {
        return security;
    }

    public Gemini getGemini() {
        return gemini;
    }

    public Cloudinary getCloudinary() {
        return cloudinary;
    }

    public static class Security {
        private boolean requireAppCheck = true;
        private String firebaseProjectNumber;

        public boolean isRequireAppCheck() {
            return requireAppCheck;
        }

        public void setRequireAppCheck(boolean requireAppCheck) {
            this.requireAppCheck = requireAppCheck;
        }

        public String getFirebaseProjectNumber() {
            return firebaseProjectNumber;
        }

        public void setFirebaseProjectNumber(String firebaseProjectNumber) {
            this.firebaseProjectNumber = firebaseProjectNumber;
        }
    }

    public static class Gemini {
        private String apiKey;
        private String model = "gemini-1.5-flash";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static class Cloudinary {
        private String cloudName;
        private String apiKey;
        private String apiSecret;
        private String uploadFolder = "soulmate_uploads";

        public String getCloudName() {
            return cloudName;
        }

        public void setCloudName(String cloudName) {
            this.cloudName = cloudName;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiSecret() {
            return apiSecret;
        }

        public void setApiSecret(String apiSecret) {
            this.apiSecret = apiSecret;
        }

        public String getUploadFolder() {
            return uploadFolder;
        }

        public void setUploadFolder(String uploadFolder) {
            this.uploadFolder = uploadFolder;
        }
    }
}
