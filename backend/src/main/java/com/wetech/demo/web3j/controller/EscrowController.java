package com.wetech.demo.web3j.controller;

import com.wetech.demo.web3j.service.EscrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/escrow")
@RequiredArgsConstructor
public class EscrowController {
    private final EscrowService service;

    @PostMapping("/deploy")
    public EscrowService.ContractView deploy() { return service.deploy(); }

    @PostMapping("/load")
    public EscrowService.ContractView load(@RequestParam("address") String address,
                                           @RequestParam("fromBlock") String fromBlock) {
        return service.load(address, fromBlock);
    }

    @GetMapping("/address")
    public EscrowService.ContractView address() { return service.address(); }

    @GetMapping("/orders/{id}")
    public EscrowService.OrderView order(@PathVariable("id") String id) { return service.order(id); }

    @GetMapping("/orders")
    public List<EscrowService.CreatedOrderView> orders() { return service.orders(); }

    @GetMapping("/orders/{id}/events")
    public List<com.wetech.demo.web3j.service.EscrowEventIndexService.EventView> events(@PathVariable("id") String id) {
        return service.events(id);
    }

    @PostMapping("/orders/{id}/resolve")
    public EscrowService.ReceiptView resolve(@PathVariable("id") String id,
                                             @RequestParam("sellerWins") String sellerWins) {
        return service.resolve(id, sellerWins);
    }
}
