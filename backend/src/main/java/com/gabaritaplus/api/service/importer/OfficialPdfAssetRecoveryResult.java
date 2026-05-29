package com.gabaritaplus.api.service.importer;

import java.util.List;

public record OfficialPdfAssetRecoveryResult(
        int recoveredAssets,
        List<String> warnings,
        List<String> errors,
        OfficialPdfAssetRecoveryDiagnostics diagnostics
) {
}
