package uk.co.armedpineapple.innoextract;

interface IExtractService {

    void extract(final String toExtract, final String extractDir,
            final ExtractCallback callback);

    void check(final String toExtract, final CheckCallback callback);

    interface ExtractCallback {
        void onProgress(int value, int max);

        void onSuccess();

        void onFailure(Exception e);
    }

    interface CheckCallback {
        void onResult(boolean valid);
    }

}
