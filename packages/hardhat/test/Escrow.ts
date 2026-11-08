import { expect } from "chai";
import { ethers } from "hardhat";
import {
  loadFixture,
  time,
} from "@nomicfoundation/hardhat-network-helpers";

const AMOUNT = ethers.parseUnits("30", 18);
const INITIAL_BALANCE = ethers.parseUnits("100", 18);

async function deployFixture() {
  const [deployer, buyer, seller, arbiter, stranger] =
    await ethers.getSigners();

  if (!deployer || !buyer || !seller || !arbiter || !stranger) {
    throw new RangeError("测试需要至少五个账户");
  }

  const token = await ethers.deployContract("WYFToken");
  await token.waitForDeployment();

  await token.mint(buyer.address, INITIAL_BALANCE);

  const escrow = await ethers.deployContract("Escrow", [
    arbiter.address,
  ]);
  await escrow.waitForDeployment();

  const deadline = (await time.latest()) + 3600;

  return {
    token,
    escrow,
    deployer,
    buyer,
    seller,
    arbiter,
    stranger,
    deadline,
  };
}

async function createdFixture() {
  const data = await deployFixture();

  await data.escrow.connect(data.buyer).createOrder(
    data.seller.address,
    await data.token.getAddress(),
    AMOUNT,
    data.deadline,
    "一本书",
  );

  return data;
}

async function paidFixture() {
  const data = await createdFixture();

  await data.token.connect(data.buyer).approve(
    await data.escrow.getAddress(),
    AMOUNT,
  );
  await data.escrow.connect(data.buyer).deposit(0);

  return data;
}

async function disputedFixture() {
  const data = await paidFixture();

  await data.escrow.connect(data.seller).dispute(0);

  return data;
}

describe("Escrow", function () {
  it("createOrder 记录订单并发出事件", async function () {
    const { token, escrow, buyer, seller, deadline } =
      await loadFixture(deployFixture);

    await expect(
      escrow.connect(buyer).createOrder(
        seller.address,
        await token.getAddress(),
        AMOUNT,
        deadline,
        "一本书",
      ),
    )
      .to.emit(escrow, "OrderCreated")
      .withArgs(
        0,
        buyer.address,
        seller.address,
        await token.getAddress(),
        AMOUNT,
        deadline,
      );

    const order = await escrow.getOrder(0);

    expect(order.buyer).to.equal(buyer.address);
    expect(order.seller).to.equal(seller.address);
    expect(order.token).to.equal(await token.getAddress());
    expect(order.amount).to.equal(AMOUNT);
    expect(order.deadline).to.equal(BigInt(deadline));
    expect(order.status).to.equal(0n);
    expect(order.description).to.equal("一本书");
    expect(await escrow.orderCount()).to.equal(1n);
  });

  it("未 approve 时 deposit 失败", async function () {
    const { token, escrow, buyer } =
      await loadFixture(createdFixture);

    await expect(
      escrow.connect(buyer).deposit(0),
    ).to.be.revertedWith("WYFToken: insufficient allowance");

    expect((await escrow.getOrder(0)).status).to.equal(0n);
    expect(await token.balanceOf(buyer.address)).to.equal(
      INITIAL_BALANCE,
    );
    expect(
      await token.balanceOf(await escrow.getAddress()),
    ).to.equal(0n);
  });

  it("approve 后 deposit 成功，代币进入 Escrow，状态变为 Paid", async function () {
    const { token, escrow, buyer } =
      await loadFixture(createdFixture);

    await token.connect(buyer).approve(
      await escrow.getAddress(),
      AMOUNT,
    );

    await expect(escrow.connect(buyer).deposit(0))
      .to.emit(escrow, "OrderPaid")
      .withArgs(0);

    expect((await escrow.getOrder(0)).status).to.equal(1n);
    expect(
      await token.balanceOf(await escrow.getAddress()),
    ).to.equal(AMOUNT);
    expect(await token.balanceOf(buyer.address)).to.equal(
      INITIAL_BALANCE - AMOUNT,
    );
  });

  it("非买家调用 confirm 失败", async function () {
    const { escrow, seller } = await loadFixture(paidFixture);

    await expect(
      escrow.connect(seller).confirm(0),
    ).to.be.revertedWith("Escrow: caller is not the buyer");

    expect((await escrow.getOrder(0)).status).to.equal(1n);
  });

  it("confirm 后卖家收到代币，状态变为 Completed", async function () {
    const { token, escrow, buyer, seller } =
      await loadFixture(paidFixture);

    await expect(escrow.connect(buyer).confirm(0))
      .to.emit(escrow, "OrderCompleted")
      .withArgs(0);

    expect((await escrow.getOrder(0)).status).to.equal(2n);
    expect(await token.balanceOf(seller.address)).to.equal(AMOUNT);
    expect(
      await token.balanceOf(await escrow.getAddress()),
    ).to.equal(0n);
  });

  it("deadline 之前 refund 失败，deadline 之后 refund 成功并退回买家", async function () {
    const { token, escrow, buyer, deadline } =
      await loadFixture(paidFixture);

    await expect(
      escrow.connect(buyer).refund(0),
    ).to.be.revertedWith("Escrow: deadline not reached");

    expect((await escrow.getOrder(0)).status).to.equal(1n);
    expect(
      await token.balanceOf(await escrow.getAddress()),
    ).to.equal(AMOUNT);

    await time.increaseTo(deadline + 1);

    await expect(escrow.connect(buyer).refund(0))
      .to.emit(escrow, "OrderRefunded")
      .withArgs(0);

    expect((await escrow.getOrder(0)).status).to.equal(3n);
    expect(await token.balanceOf(buyer.address)).to.equal(
      INITIAL_BALANCE,
    );
    expect(
      await token.balanceOf(await escrow.getAddress()),
    ).to.equal(0n);
  });

  it("非仲裁人调用 resolve 失败", async function () {
    const { escrow, buyer } = await loadFixture(disputedFixture);

    await expect(
      escrow.connect(buyer).resolve(0, false),
    ).to.be.revertedWith("Escrow: caller is not the arbiter");

    expect((await escrow.getOrder(0)).status).to.equal(4n);
  });

  it("dispute 后仲裁人 resolve(false) 退款给买家，状态变为 Refunded", async function () {
    const { token, escrow, buyer, seller, arbiter } =
      await loadFixture(paidFixture);

    await expect(escrow.connect(seller).dispute(0))
      .to.emit(escrow, "OrderDisputed")
      .withArgs(0, seller.address);

    expect((await escrow.getOrder(0)).status).to.equal(4n);

    await expect(escrow.connect(arbiter).resolve(0, false))
      .to.emit(escrow, "OrderResolved")
      .withArgs(0, false);

    expect((await escrow.getOrder(0)).status).to.equal(3n);
    expect(await token.balanceOf(buyer.address)).to.equal(
      INITIAL_BALANCE,
    );
    expect(await token.balanceOf(seller.address)).to.equal(0n);
    expect(
      await token.balanceOf(await escrow.getAddress()),
    ).to.equal(0n);
  });
});