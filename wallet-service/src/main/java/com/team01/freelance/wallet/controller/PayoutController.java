package com.team01.freelance.wallet.controller;

import com.team01.freelance.wallet.dto.PayoutDetailsDTO;
import com.team01.freelance.wallet.dto.PromoCodeUsageDTO;
import com.team01.freelance.wallet.model.Payout;
import com.team01.freelance.wallet.service.PayoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payouts")
public class PayoutController {

    @Autowired
    private PayoutService payoutService;

    @GetMapping
    public ResponseEntity<List<Payout>> getAllPayouts() {
        return ResponseEntity.ok(payoutService.getAllPayouts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payout> getPayoutById(@PathVariable Long id) {
        return payoutService.getPayoutById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Payout> createPayout(@RequestBody Payout payout) {
        return ResponseEntity.ok(payoutService.createPayout(payout));
    }

    /**
     * Updates a payout by ID.
     *
     * @param id the payout ID
     * @param payout the update payload
     * @return 200 with updated payout, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Payout> updatePayout(@PathVariable Long id, @RequestBody Payout payout) {
        try {
            return ResponseEntity.ok(payoutService.updatePayout(id, payout));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayoutById(@PathVariable Long id) {
        if (payoutService.deletePayoutById(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllPayouts() {
        payoutService.deleteAllPayouts();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{payoutId:\\d+}/details")
    public ResponseEntity<PayoutDetailsDTO> getDetails(@PathVariable Long payoutId) {
        return ResponseEntity.ok(payoutService.getPayoutDetails(payoutId));
    }

    // Create promo code task is required to complete getTopUsedPromos
    @GetMapping("/promos/top-used")
    public ResponseEntity<List<PromoCodeUsageDTO>> getTopUsedPromos(
            @RequestParam int limit) {

        return ResponseEntity.ok(payoutService.getTopUsedPromoCodes(limit));
    }
}
