package br.edu.foodnow.integration;

import org.springframework.stereotype.Component;

@Component
public class FakeCourierClient {

    public CourierDispatch solicitarEntregador(CourierRequest request) {
        if (request.distanceKm() > 40 || "EXTERNA".equals(request.providerZone())) {
            return new CourierDispatch(null, "NO_COURIER_AVAILABLE", 0);
        }
        String codigo = "COURIER-" + request.orderId();
        int espera = "CENTRAL".equals(request.providerZone()) ? 4 : 9;
        return new CourierDispatch(codigo, "DRIVER_ASSIGNED", espera);
    }

    public record CourierRequest(Long orderId, double distanceKm, String providerZone,
                                 String destinationCoordinates) {
    }

    public record CourierDispatch(String courierCode, String providerStatus, int pickupEtaMinutes) {
    }
}
