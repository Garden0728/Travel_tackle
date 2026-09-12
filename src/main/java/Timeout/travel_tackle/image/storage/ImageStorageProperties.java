package Timeout.travel_tackle.image.storage;

public record ImageStorageProperties(
        String bucket,
        String region,
        String publicBaseUrl // CloudFront 등 읽기용 도메인. 비어 있으면 S3 가상 호스팅 URL 사용
) {
    public String baseUrl() {
        return publicBaseUrl == null || publicBaseUrl.isBlank()
                ? "https://" + bucket + ".s3." + region + ".amazonaws.com"
                : publicBaseUrl.replaceAll("/+$", "");
    }

    public String publicUrlOf(String key) {
        return baseUrl() + "/" + key;
    }

    /** 우리 저장소 URL 이면 객체 키를, 아니면(외부 URL) null 을 돌려준다. */
    public String keyOf(String imageUrl) {
        String prefix = baseUrl() + "/";
        return imageUrl != null && imageUrl.startsWith(prefix) ? imageUrl.substring(prefix.length()) : null;
    }
}
