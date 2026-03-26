package com.enterprise.knowledgeai.tenant.service;

import com.enterprise.knowledgeai.tenant.entity.Tenant;
import com.enterprise.knowledgeai.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    public Tenant createTenant(String companyName) {
        String baseSlug = companyName.toLowerCase().replaceAll("[^a-z0-9]", "-");
        String slug = baseSlug;
        int counter = 1;
        while (tenantRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        Tenant tenant = Tenant.builder()
                .name(companyName)
                .slug(slug)
                .plan(Tenant.Plan.FREE)
                .build();

        return tenantRepository.save(tenant);
    }

    public Tenant findById(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
    }
}
