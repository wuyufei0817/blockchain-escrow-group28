import type { HardhatRuntimeEnvironment } from "hardhat/types";
import type { DeployFunction } from "hardhat-deploy/types";

const EXPECTED_DEPLOYER =
  "0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6";

const deployToken: DeployFunction = async function (
  hre: HardhatRuntimeEnvironment,
) {
  const { deployer } = await hre.getNamedAccounts();

  if (deployer?.toLowerCase() !== EXPECTED_DEPLOYER.toLowerCase()) {
    throw new TypeError("部署账户不符，请检查当前终端的私钥环境变量");
  }

  console.log("部署者：", deployer);

  await hre.deployments.deploy("WYFToken", {
    from: deployer,
    args: [],
    log: true,
    autoMine: true,
  });
};

export default deployToken;
deployToken.tags = ["TokenWYF202492451119"];
