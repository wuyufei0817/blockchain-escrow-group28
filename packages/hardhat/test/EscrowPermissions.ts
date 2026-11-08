import { expect } from "chai";
import { lifecycleActions } from "../../nextjs/utils/escrow/orderPermissions";
import type { LifecycleAction, OrderParties } from "../../nextjs/utils/escrow/orderPermissions";

const buyer = "0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6";
const seller = "0x92b0f84D625437245c3296012A0784d10C560309";
const arbiter = "0x8c1f39320b2dabf58ecead5225931674d558d3ed";
const outsider = "0x0000000000000000000000000000000000000001";
const roles = { buyer, seller, arbiter, outsider, disconnected: undefined } as const;

describe("Escrow lifecycle role permissions", () => {
  for (const status of [0, 1, 2, 3, 4, 99]) {
    for (const [role, account] of Object.entries(roles)) {
      it(`shows only allowed actions when status=${status} and role=${role}`, () => {
        // Given: one concrete role and a current order status.
        const order: OrderParties = { buyer, seller, status };
        const expected: readonly LifecycleAction[] = status === 1 && role === "buyer" ? ["confirm", "dispute"]
          : status === 1 && role === "seller" ? ["dispute"]
          : status === 4 && role === "arbiter" ? ["resolveSeller", "resolveBuyer"] : [];
        // When: the UI and write guard ask for allowed actions.
        const actual = lifecycleActions(order, arbiter, account);
        // Then: no other role or status receives these privileges.
        expect(actual).to.deep.equal(expected);
      });
    }
  }

  it("compares addresses case insensitively when identifying the buyer", () => {
    // Given: the wallet returns a lowercase spelling of the same address.
    const account = "0x898ed6f95ef783ad4292d5ff2351f2dc7936adf6";
    // When: available actions are derived for a Paid order.
    const actual = lifecycleActions({ buyer, seller, status: 1 }, arbiter, account);
    // Then: the buyer retains both permitted actions.
    expect(actual).to.deep.equal(["confirm", "dispute"]);
  });

  it("does not expose arbitration when the arbiter has not loaded", () => {
    // Given / When: a disputed order has no known arbiter configuration yet.
    const actual = lifecycleActions({ buyer, seller, status: 4 }, undefined, buyer);
    // Then: arbitration stays unavailable.
    expect(actual).to.deep.equal([]);
  });
});
