package eu.fuelfleet.subscription.service;

import eu.fuelfleet.subscription.entity.Plan;
import eu.fuelfleet.subscription.entity.Subscription;
import eu.fuelfleet.subscription.entity.SubscriptionStatus;
import eu.fuelfleet.subscription.repository.SubscriptionRepository;
import eu.fuelfleet.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SubscriptionGuard {

    private final SubscriptionRepository subscriptionRepository;
    private final VehicleRepository vehicleRepository;

    private static final int STARTER_VEHICLE_LIMIT = 5;
    private static final int GROWTH_VEHICLE_LIMIT = 25;

    public boolean canAddVehicle(UUID companyId) {
        Optional<Subscription> subOpt = subscriptionRepository.findByCompanyId(companyId);
        if (subOpt.isEmpty()) {
            return false;
        }

        Subscription subscription = subOpt.get();
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE
                && subscription.getStatus() != SubscriptionStatus.TRIALING) {
            return false;
        }

        long currentCount = vehicleRepository.countByCompanyIdAndActiveTrue(companyId);

        return switch (subscription.getPlan()) {
            case STARTER -> currentCount < STARTER_VEHICLE_LIMIT;
            case GROWTH -> currentCount < GROWTH_VEHICLE_LIMIT;
            case ENTERPRISE -> true;
        };
    }

    public boolean isActive(UUID companyId) {
        return subscriptionRepository.findByCompanyId(companyId)
                .map(sub -> sub.getStatus() == SubscriptionStatus.ACTIVE
                        || sub.getStatus() == SubscriptionStatus.TRIALING)
                .orElse(false);
    }
}
