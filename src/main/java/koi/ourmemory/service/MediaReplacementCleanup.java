package koi.ourmemory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Keep the old asset until the database transaction commits successfully. */
@Service
@RequiredArgsConstructor
public class MediaReplacementCleanup {
    private final CloudinaryService cloudinaryService;

    public void register(String oldId, String newId, String resourceType) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    if (oldId != null && !oldId.equals(newId)) cloudinaryService.deleteResource(oldId, resourceType);
                } else if (status == STATUS_ROLLED_BACK && newId != null) {
                    cloudinaryService.deleteResource(newId, resourceType);
                }
            }
        });
    }
}
