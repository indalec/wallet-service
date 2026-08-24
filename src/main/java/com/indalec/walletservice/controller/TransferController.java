package com.indalec.walletservice.controller;

import com.indalec.walletservice.dto.CreateTransferRequest;
import com.indalec.walletservice.model.Transfer;
import com.indalec.walletservice.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public Transfer createTransfer(
            // Valid checks the validation rules from CreateTransferRequest,RequestBody converts the JSON from the HTTP to an Object
            @Valid @RequestBody CreateTransferRequest request
    ) {
        return transferService.transfer(
                request.sourceWalletId(),
                request.destinationWalletId(),
                request.amount(),
                request.currency(),
                request.idempotencyKey()
        );
    }
}