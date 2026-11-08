import { expect } from "chai";
import { createOrderSchema, orderDeadline, UINT256_MAX } from "../../nextjs/utils/escrow/orderForm";

const buyer = "0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6";
const seller = "0x92b0f84D625437245c3296012A0784d10C560309";
const valid = { seller, amount: "30", minutes: "30", description: "页面订单" };

describe("Escrow order form", () => {
  it("parses 30 tokens as 18-decimal units and minutes as an exact integer", () => {
    // Given: the user's valid course order form.
    const schema = createOrderSchema(buyer);
    // When: the form crosses the parsing boundary.
    const result = schema.safeParse(valid);
    // Then: values are ready for ABI encoding without floating-point arithmetic.
    expect(result.success).to.equal(true);
    if (!result.success) throw result.error;
    expect(result.data.amount).to.equal(30n * 10n ** 18n);
    expect(result.data.minutes).to.equal(30n);
  });

  for (const [field, value] of [
    ["seller", "0x0000000000000000000000000000000000000000"],
    ["seller", buyer], ["seller", "wrong"],
    ["amount", "0"], ["amount", "-1"], ["amount", "1e3"],
    ["amount", "0.0000000000000000001"], ["amount", UINT256_MAX.toString()],
    ["minutes", "0"], ["minutes", "1.5"], ["description", "   "],
  ] as const) {
    it(`rejects invalid ${field}=${value}`, () => {
      // Given: one invalid field in an otherwise valid form.
      const input = { ...valid, [field]: value };
      // When: validation is applied before opening a wallet request.
      const result = createOrderSchema(buyer).safeParse(input);
      // Then: no ABI arguments are accepted.
      expect(result.success).to.equal(false);
    });
  }

  it("derives the deadline from chain time rather than the computer clock", () => {
    // Given: a chain timestamp and a 30-minute duration.
    const blockTimestamp = 10000n;
    // When: the transaction deadline is prepared.
    const deadline = orderDeadline(blockTimestamp, 30n);
    // Then: it is exactly 1800 chain seconds later.
    expect(deadline).to.equal(11800n);
  });

  it("rejects a deadline that would exceed uint256", () => {
    // Given: a duration that overflows when added to the block timestamp.
    // When: a deadline is prepared.
    // Then: the invalid transaction is rejected locally.
    expect(() => orderDeadline(UINT256_MAX - 1n, 1n)).to.throw();
  });
});
