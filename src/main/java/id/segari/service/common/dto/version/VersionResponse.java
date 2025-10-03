package id.segari.service.common.dto.version;

public record VersionResponse(
        String javaVersion,
        String javaVendor,
        String javaRuntimeVersion,
        String javaVmName,
        String javaVmVersion,
        String javaSpecificationVersion
) {
}