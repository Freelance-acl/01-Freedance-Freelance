package com.team01.freelance.wallet.controller;

import com.team01.freelance.wallet.model.PayoutPromo;
import com.team01.freelance.wallet.service.PayoutPromoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payout-promos")
public class PayoutPromoController {

    @Autowired
    private PayoutPromoService payoutPromoService;

    @GetMapping
    public ResponseEntity<List<PayoutPromo>> getAllPayoutPromos() {
        return ResponseEntity.ok(payoutPromoService.getAllPayoutPromos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PayoutPromo> getPayoutPromoById(@PathVariable Long id) {
        return payoutPromoService.getPayoutPromoById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PayoutPromo> createPayoutPromo(@RequestBody PayoutPromo payoutPromo) {
        return ResponseEntity.ok(payoutPromoService.createPayoutPromo(payoutPromo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PayoutPromo> updatePayoutPromo(@PathVariable Long id, @RequestBody PayoutPromo payoutPromo) {
        return payoutPromoService.updatePayoutPromo(id, payoutPromo)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayoutPromoById(@PathVariable Long id) {
        if (payoutPromoService.deletePayoutPromoById(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllPayoutPromos() {
        payoutPromoService.deleteAllPayoutPromos();
        return ResponseEntity.noContent().build();
    }
}
