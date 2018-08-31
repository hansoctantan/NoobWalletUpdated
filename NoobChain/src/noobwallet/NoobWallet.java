package noobwallet;

import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;

public class NoobWallet {

    public static ArrayList<Block> blockchain = new ArrayList<Block>();
    public static HashMap<String, TransactionOutput> UTXOs = new HashMap<String, TransactionOutput>();

    //public static int difficulty = 3;
    public static float minimumTransaction = 0.1f;
    public static Wallet walletA;
    public static Wallet walletB;
    public static Wallet coinbase;
    public static Wallet miner;
    public static Transaction genesisTransaction;

    public static void main(String[] args) {
        //add our blocks to the blockchain ArrayList:
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()); //Setup Bouncey castle as a Security Provider

        //Create wallets:
        walletA = new Wallet();
        walletB = new Wallet();
        coinbase = new Wallet();
        miner = new Wallet();

        //create genesis transaction, which sends 100 NoobCoin to walletA: 
        genesisTransaction = new Transaction(coinbase.publicKey, walletA.publicKey, 100f, null);
        genesisTransaction.generateSignature(coinbase.privateKey);	 //manually sign the genesis transaction	
        genesisTransaction.transactionId = "0"; //manually set the transaction id
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value, genesisTransaction.transactionId)); //manually add the Transactions Output
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); //its important to store our first transaction in the UTXOs list.

        System.out.println("Creating and Mining Genesis block... ");
        Block genesis = new Block("0");
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);

        //testing
        Block block1 = new Block(blockchain.get(blockchain.size() - 1).hash);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("\nWalletA is Attempting to send funds (40) to WalletB...");
        block1.addTransaction(walletA.sendFunds(walletB.publicKey, 40f));       
        addBlock(block1);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());
        System.out.println("Miners's balance is: " + miner.getBalance());

        Block block2 = new Block(blockchain.get(blockchain.size() - 1).hash);
        System.out.println("\nWalletA Attempting to send more funds (1000) than it has...");
        block2.addTransaction(walletA.sendFunds(walletB.publicKey, 1000f));
        addBlock(block2);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());
        System.out.println("Miners's balance is: " + miner.getBalance());
       
        Block block3 = new Block(blockchain.get(blockchain.size() - 1).hash);
        System.out.println("\nWalletB is Attempting to send funds (20) to WalletA...");
        block3.addTransaction(walletB.sendFunds(walletA.publicKey, 20));
        addBlock(block3);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());
        System.out.println("Miners's balance is: " + miner.getBalance());

//        block2.mineStartTimeStamp = new Date().getTime();
//        block2.hash = block2.calculateHash();
//        System.err.println(block2.name + " = " + block2.hash + "<<<");
        
        isChainValid();

    }

    public static Boolean isChainValid() {
        Block currentBlock;
        Block previousBlock;
        //commented String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        HashMap<String, TransactionOutput> tempUTXOs = new HashMap<>(); //a temporary working list of unspent transactions at a given block state.
        tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        //loop through blockchain to check hashes:
        for (int i = 1; i < blockchain.size(); i++) {
            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i - 1);
            
            //compare registered hash and calculated hash:
            if (!currentBlock.hash.equals(currentBlock.calculateHash())) {
                System.out.println("#Current Hashes not equal");
                return false;
            }
            //compare previous hash and registered previous hash
            if (!previousBlock.hash.equals(currentBlock.previousHash)) {
                System.out.println("#Previous Hashes not equal");
                return false;
            }
            //check if hash is solved
            if (!currentBlock.mined) { //(!currentBlock.hash.substring(0, difficulty).equals(hashTarget)) { //edited
                System.out.println("#This block hasn't been mined");
                return false;
            }

            //loop thru blockchains transactions:
            TransactionOutput tempOutput;
            for (int t = 0; t < currentBlock.transactions.size(); t++) {
                Transaction currentTransaction = currentBlock.transactions.get(t);

                if (!currentTransaction.verifySignature()) {
                    System.out.println("#Signature on Transaction(" + t + ") is Invalid");
                    return false;
                }
                if(!currentTransaction.asReward) { //added
                    if (currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                        System.out.println("#Inputs are note equal to outputs on Transaction(" + t + ")");
                        return false;
                    }
                
                    for (TransactionInput input : currentTransaction.inputs) {
                        tempOutput = tempUTXOs.get(input.transactionOutputId);

                        if (tempOutput == null) {
                            System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
                            return false;
                        }

                        if (input.UTXO.value != tempOutput.value) {
                            System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
                            return false;
                        }

                        tempUTXOs.remove(input.transactionOutputId);
                    }
                } //added  
                
                    for (TransactionOutput output : currentTransaction.outputs) {
                        tempUTXOs.put(output.id, output);
                    }

                    if (currentTransaction.outputs.get(0).reciepient != currentTransaction.reciepient) {
                        System.out.println("#Transaction(" + t + ") output reciepient is not who it should be");
                        return false;
                    }
                
                if(!currentTransaction.asReward) { //added
                    if (currentTransaction.outputs.get(1).reciepient != currentTransaction.sender) {
                        System.out.println("#Transaction(" + t + ") output 'change' is not sender.");
                        return false;
                    }
                } //added  

            }

        }
        System.out.println("Blockchain is valid");
        return true;
    }

    public static void addBlock(Block newBlock) {
        //add
        int difficulty = blockchain.size() / 100000;
        if(difficulty < 3)
            difficulty = 3;
        else if(difficulty > 10)
            difficulty = 10;
        //add
        
        newBlock.mineBlock(difficulty);
        
        //add
        if(!newBlock.previousHash.equals("0")) {
            Transaction rewardTransaction = new Transaction(coinbase.publicKey, miner.publicKey, 1f, null);
            rewardTransaction.asReward = true;
            rewardTransaction.generateSignature(coinbase.privateKey);	 //manually sign the genesis transaction	
            rewardTransaction.transactionId = rewardTransaction.calulateHash(); //manually set the transaction id
            rewardTransaction.outputs.add(new TransactionOutput(rewardTransaction.reciepient, rewardTransaction.value, rewardTransaction.transactionId)); //manually add the Transactions Output
            UTXOs.put(rewardTransaction.outputs.get(0).id, rewardTransaction.outputs.get(0)); //its important to store our first transaction in the UTXOs list.
            newBlock.addTransaction(rewardTransaction);
        }
        //add
        
        blockchain.add(newBlock);
    }
}
