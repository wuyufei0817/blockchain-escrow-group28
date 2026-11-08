import type { HardhatRuntimeEnvironment } from "hardhat/types";
import type { DeployFunction } from "hardhat-deploy/types";

const EXPECTED_DEPLOYER =
  "0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6";

const ARBITER =
  "0x8c1f39320b2dabf58ecead5225931674d558d3ed";

const deployEscrow: DeployFunction = async function (
  hre: HardhatRuntimeEnvironment,
) {
  const { deployer } = await hre.getNamedAccounts();

  if (deployer?.toLowerCase() !== EXPECTED_DEPLOYER.toLowerCase()) {
    throw new TypeError("部署账户不符，请检查当前终端的私钥环境变量");
  }

  console.log("部署者：", deployer);
  console.log("仲裁人：", ARBITER);

  await hre.deployments.deploy("Escrow", {
    from: deployer,
    args: [ARBITER],
    log: true,
    autoMine: true,
  });
};

export default deployEscrow;
deployEscrow.tags = ["EscrowWYF202492451119"];
