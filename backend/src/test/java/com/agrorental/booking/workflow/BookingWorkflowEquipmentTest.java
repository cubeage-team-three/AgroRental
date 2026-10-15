package com.agrorental.booking.workflow;

import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.partner.entity.Partner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Item 1 of the booking workflow verification: public equipment listing/search/detail, partner equipment
 * creation/update/disable restricted to the owner, request validation, and the rule that disabled, INACTIVE and
 * UNDER_MAINTENANCE equipment cannot be booked.
 */
@DisplayName("Booking workflow 1: equipment listing, ownership and bookability")
class BookingWorkflowEquipmentTest extends AbstractBookingWorkflowTest {

    private static String createJson(String name, String rentalPrice, Long bodyPartnerId) {
        return """
                {"name": "%s", "category": "TRACTOR", "brand": "Sonalika", "model": "DI 745", "manufacturingYear": 2022,
                 "capacity": "50 HP", "rentalPrice": %s, "fuelType": "DIESEL", "description": "Well maintained",
                 "partnerId": %d, "locationAddress": "Baramati, Pune", "latitude": 18.15, "longitude": 74.58,
                 "images": [{"imageUrl": "/uploads/equipment/t.jpg", "isPrimary": true, "displayOrder": 1}]}
                """.formatted(name, rentalPrice, bodyPartnerId);
    }

    /** The full payload the partner UI (AddEquipment edit mode / EquipmentAvailability) sends on PUT. */
    static String updateJson(Equipment e, String name, String rentalPrice, AvailabilityStatus status, boolean disabled) {
        return """
                {"name": "%s", "category": "TRACTOR", "brand": "%s", "model": "%s", "manufacturingYear": 2023,
                 "capacity": "%s", "rentalPrice": %s, "fuelType": "DIESEL", "description": "Updated description",
                 "locationAddress": "%s", "latitude": 18.52, "longitude": 73.85, "availabilityStatus": "%s",
                 "isDisabled": %s, "maintenanceNotes": null,
                 "images": [{"imageUrl": "/uploads/equipment/t.jpg", "isPrimary": true, "displayOrder": 1}]}
                """.formatted(name, e.getBrand(), e.getModel(), e.getCapacity(), rentalPrice, e.getLocationAddress(),
                status.name(), disabled);
    }

    private static Equipment disabledAt(Equipment equipment, String village) {
        equipment.setLocationAddress(village + ", Pune");
        return equipment;
    }

    @Test
    @DisplayName("Listing, search and detail are public; disabled equipment is hidden from search and discovery")
    void publicListingSearchAndDetail() throws Exception {
        Partner partner = newPartner();
        Equipment visible = newEquipment(partner);
        Equipment disabled = newEquipment(partner, b -> b.isDisabled(true));

        // Both machines share the owner's unique village, so the location filter isolates them from other tests' rows
        String village = visible.getLocationAddress().split(",")[0];
        equipmentRepository.save(disabledAt(disabled, village));
        MvcResult search = call(get("/api/equipment/search/page").param("category", "TRACTOR")
                .param("locationAddress", village).param("size", "100"), null);
        assertThat(status(search)).isEqualTo(200);
        List<Integer> searchIds = json(search, "$.data.content[*].id");
        assertThat(searchIds).contains(visible.getId().intValue()).doesNotContain(disabled.getId().intValue());

        MvcResult detail = anonymousGet("/api/equipment/" + visible.getId());
        assertThat(status(detail)).isEqualTo(200);
        assertThat((String) json(detail, "$.data.name")).isEqualTo(visible.getName());
        assertThat((Integer) json(detail, "$.data.partnerId")).isEqualTo(partner.getId().intValue());

        MvcResult discovery = anonymousGet("/api/equipment/available/page?size=100");
        assertThat(status(discovery)).isEqualTo(200);
        List<Integer> discoverableIds = json(discovery, "$.data.content[*].id");
        assertThat(discoverableIds).doesNotContain(disabled.getId().intValue());

        assertThat(status(anonymousGet("/api/equipment/999999999"))).isEqualTo(404);
    }

    @Test
    @DisplayName("The admin catalog (all equipment, disabled included) is admin-only")
    void adminCatalogIsAdminOnly() throws Exception {
        Partner partner = newPartner();
        Equipment disabled = newEquipment(partner, b -> b.isDisabled(true));

        assertThat(status(anonymousGet("/api/equipment/page?size=5"))).isIn(401, 403);
        assertThat(status(anonymousGet("/api/equipment"))).isIn(401, 403);
        assertThat(status(call(get("/api/equipment/page"), tokenFor(partner)))).isEqualTo(403);
        assertThat(status(call(get("/api/equipment"), tokenFor(newFarmer())))).isEqualTo(403);

        MvcResult admin = call(get("/api/equipment/page").param("size", "100").param("sort", "id,desc"), tokenFor(newAdmin()));
        assertThat(status(admin)).isEqualTo(200);
        List<Integer> ids = json(admin, "$.data.content[*].id");
        assertThat(ids).contains(disabled.getId().intValue());
    }

    @Test
    @DisplayName("A partner creates equipment owned by the token's partner (body partnerId ignored), AVAILABLE and enabled")
    void partnerCreatesOwnEquipment() throws Exception {
        Partner owner = newPartner();
        Partner other = newPartner();

        MvcResult created = callJson(post("/api/equipment"), tokenFor(owner), createJson("Sonalika DI 745", "1800.00", other.getId()));

        assertThat(status(created)).as(body(created)).isEqualTo(201);
        assertThat((Integer) json(created, "$.data.partnerId")).isEqualTo(owner.getId().intValue());
        assertThat((String) json(created, "$.data.availabilityStatus")).isEqualTo("AVAILABLE");
        assertThat((Boolean) json(created, "$.data.isDisabled")).isFalse();
        Long id = ((Number) json(created, "$.data.id")).longValue();
        assertThat(equipmentRepository.findById(id).orElseThrow().getPartner().getId()).isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("Equipment creation validates required fields, a positive price and at least one image")
    void equipmentCreationValidation() throws Exception {
        Partner owner = newPartner();
        long before = equipmentRepository.count();

        MvcResult zeroPrice = callJson(post("/api/equipment"), tokenFor(owner), createJson("Zero price", "0", owner.getId()));
        assertThat(status(zeroPrice)).isEqualTo(400);
        assertThat((String) json(zeroPrice, "$.data.rentalPrice")).contains("greater than zero");

        MvcResult noName = callJson(post("/api/equipment"), tokenFor(owner), createJson("", "1500.00", owner.getId()));
        assertThat(status(noName)).isEqualTo(400);
        assertThat((String) json(noName, "$.data.name")).isNotBlank();

        String noImages = createJson("No images", "1500.00", owner.getId()).replaceAll("\"images\": \\[.*?]", "\"images\": []");
        MvcResult withoutImages = callJson(post("/api/equipment"), tokenFor(owner), noImages);
        assertThat(status(withoutImages)).isEqualTo(400);

        assertThat(equipmentRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("Farmers, operators and anonymous callers cannot create equipment")
    void onlyPartnersCreateEquipment() throws Exception {
        Partner owner = newPartner();
        long before = equipmentRepository.count();
        String payload = createJson("Not allowed", "1500.00", owner.getId());

        assertThat(status(callJson(post("/api/equipment"), tokenFor(newFarmer()), payload))).isEqualTo(403);
        assertThat(status(callJson(post("/api/equipment"), tokenFor(newOperator()), payload))).isEqualTo(403);
        assertThat(status(callJson(post("/api/equipment"), null, payload))).isIn(401, 403);
        assertThat(equipmentRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("Approval rules: PENDING/REJECTED partners cannot list equipment; an inactive farmer cannot book")
    void unapprovedAccountsCannotAct() throws Exception {
        Partner pending = newPartner();
        pending.setVerificationStatus(Partner.VerificationStatus.PENDING);
        partnerRepository.save(pending);
        Partner rejected = newPartner();
        rejected.setVerificationStatus(Partner.VerificationStatus.REJECTED);
        partnerRepository.save(rejected);
        long before = equipmentRepository.count();

        assertThat(status(callJson(post("/api/equipment"), tokenFor(pending), createJson("Pending partner", "1500.00", pending.getId()))))
                .isIn(401, 403);
        assertThat(status(callJson(post("/api/equipment"), tokenFor(rejected), createJson("Rejected partner", "1500.00", rejected.getId()))))
                .isIn(401, 403);
        assertThat(equipmentRepository.count()).isEqualTo(before);

        Equipment equipment = newEquipment(newPartner());
        Farmer inactive = newFarmer();
        inactive.setAccountStatus("INACTIVE");
        farmerRepository.save(inactive);
        assertThat(status(postBooking(inactive, equipment.getId(), day(3), day(4)))).isIn(401, 403);
        assertThat(bookingRepository.findByEquipmentId(equipment.getId())).isEmpty();
    }

    @Test
    @DisplayName("Equipment of a partner rejected/deactivated after listing is hidden and cannot be booked")
    void rejectedPartnersEquipmentIsNotRentable() throws Exception {
        Partner partner = newPartner();
        Equipment equipment = newEquipment(partner);
        String village = equipment.getLocationAddress().split(",")[0];
        // What PUT /api/partners/{id}/kyc/reject does today: REJECTED and inactive
        partner.setVerificationStatus(Partner.VerificationStatus.REJECTED);
        partner.setActive(false);
        partnerRepository.save(partner);

        MvcResult search = call(get("/api/equipment/search/page").param("locationAddress", village), null);
        List<Integer> searchIds = json(search, "$.data.content[*].id");
        assertThat(searchIds).doesNotContain(equipment.getId().intValue());
        MvcResult discovery = call(get("/api/equipment/available/page").param("size", "100").param("sort", "id,desc"), null);
        List<Integer> discoverableIds = json(discovery, "$.data.content[*].id");
        assertThat(discoverableIds).doesNotContain(equipment.getId().intValue());

        MvcResult booking = postBooking(newFarmer(), equipment.getId(), day(3), day(4));
        assertThat(status(booking)).isEqualTo(400);
        assertThat(bookingRepository.findByEquipmentId(equipment.getId())).isEmpty();
    }

    @Test
    @DisplayName("Only the owning partner (or an admin) can update, disable or enable equipment")
    void ownershipOfUpdatesAndToggles() throws Exception {
        Partner owner = newPartner();
        Partner other = newPartner();
        Equipment equipment = newEquipment(owner);

        MvcResult foreignUpdate = callJson(put("/api/equipment/" + equipment.getId()), tokenFor(other),
                updateJson(equipment, "Hijacked", "1.00", AvailabilityStatus.AVAILABLE, false));
        assertThat(status(foreignUpdate)).isIn(400, 403);
        MvcResult foreignDisable = call(patch("/api/equipment/" + equipment.getId() + "/disable"), tokenFor(other));
        assertThat(status(foreignDisable)).isIn(400, 403);
        assertThat(status(callJson(put("/api/equipment/" + equipment.getId()), tokenFor(newFarmer()),
                updateJson(equipment, "Hijacked", "1.00", AvailabilityStatus.AVAILABLE, false)))).isEqualTo(403);

        Equipment unchanged = equipmentRepository.findById(equipment.getId()).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo(equipment.getName());
        assertThat(unchanged.getRentalPrice()).isEqualByComparingTo(DAILY_RATE);
        assertThat(unchanged.getIsDisabled()).isFalse();

        MvcResult ownUpdate = callJson(put("/api/equipment/" + equipment.getId()), tokenFor(owner),
                updateJson(equipment, "Renamed tractor", "2700.00", AvailabilityStatus.AVAILABLE, false));
        assertThat(status(ownUpdate)).as(body(ownUpdate)).isEqualTo(200);
        assertThat(equipmentRepository.findById(equipment.getId()).orElseThrow().getRentalPrice()).isEqualByComparingTo("2700.00");

        assertThat(status(call(patch("/api/equipment/" + equipment.getId() + "/disable"), tokenFor(owner)))).isEqualTo(200);
        assertThat(equipmentRepository.findById(equipment.getId()).orElseThrow().getIsDisabled()).isTrue();
        assertThat(status(call(patch("/api/equipment/" + equipment.getId() + "/enable"), tokenFor(newAdmin())))).isEqualTo(200);
        assertThat(equipmentRepository.findById(equipment.getId()).orElseThrow().getIsDisabled()).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"DISABLED", "INACTIVE", "UNDER_MAINTENANCE"})
    @DisplayName("Disabled, INACTIVE and UNDER_MAINTENANCE equipment cannot be booked")
    void unavailableEquipmentCannotBeBooked(String state) throws Exception {
        Partner owner = newPartner();
        Equipment equipment = newEquipment(owner, b -> {
            if (state.equals("DISABLED")) {
                b.isDisabled(true);
            } else {
                b.availabilityStatus(AvailabilityStatus.valueOf(state));
            }
        });
        Farmer farmer = newFarmer();

        MvcResult result = postBooking(farmer, equipment.getId(), day(5), day(6));

        assertThat(status(result)).isEqualTo(400);
        assertThat((String) json(result, "$.message")).contains("currently unavailable");
        assertThat(bookingRepository.findByEquipmentId(equipment.getId())).isEmpty();
    }

    @Test
    @DisplayName("Equipment with active bookings cannot be deleted, even when its availability flag is stale")
    void equipmentWithActiveBookingCannotBeDeleted() throws Exception {
        Partner owner = newPartner();
        // Legacy/stale data: an active booking while the flag still says AVAILABLE
        Equipment equipment = newEquipment(owner);
        insertBooking(newFarmer(), equipment, BookingStatus.CONFIRMED, day(3), day(4));

        MvcResult result = call(delete("/api/equipment/" + equipment.getId()), tokenFor(owner));

        assertThat(status(result)).as(body(result)).isEqualTo(400);
        assertThat(body(result)).containsIgnoringCase("booking");
        assertThat(equipmentRepository.findById(equipment.getId())).isPresent();
    }

    @Test
    @DisplayName("Equipment with only past bookings cannot be deleted (history kept) and the refusal is a clear 400")
    void equipmentWithBookingHistoryIsNotDeletedSilentlyOrWithA409() throws Exception {
        Partner owner = newPartner();
        Equipment equipment = newEquipment(owner);
        insertBooking(newFarmer(), equipment, BookingStatus.COMPLETED, day(-10), day(-8));

        MvcResult result = call(delete("/api/equipment/" + equipment.getId()), tokenFor(owner));

        assertThat(status(result)).as(body(result)).isEqualTo(400);
        assertThat(body(result)).containsIgnoringCase("disable");
        assertThat(equipmentRepository.findById(equipment.getId())).isPresent();
    }

    @Test
    @DisplayName("Equipment without bookings can be deleted by its owner but not by another partner")
    void unusedEquipmentCanBeDeletedByItsOwner() throws Exception {
        Partner owner = newPartner();
        Equipment equipment = newEquipment(owner);

        assertThat(status(call(delete("/api/equipment/" + equipment.getId()), tokenFor(newPartner())))).isIn(400, 403);
        assertThat(equipmentRepository.findById(equipment.getId())).isPresent();

        assertThat(status(call(delete("/api/equipment/" + equipment.getId()), tokenFor(owner)))).isEqualTo(204);
        assertThat(equipmentRepository.findById(equipment.getId())).isEmpty();
        assertThat(status(call(get("/api/equipment/" + equipment.getId()), null))).isEqualTo(404);
    }
}
