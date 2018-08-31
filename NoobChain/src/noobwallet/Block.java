package noobwallet;

import java.util.ArrayList;
import java.util.Date;

public class Block {

    public String hash;
    public String previousHash;
    public String merkleRoot;
    public ArrayList<Transaction> transactions = new ArrayList<>(); //our data will be a simple message.
    public long timeStamp; //as number of milliseconds since 1/1/1970.
    public int nonce;
    public long mineStartTimeStamp; //added
    public long mineEndTimeStamp; //added
    public String dificultyString = ""; //added
    public boolean mined = false; //added

    //Block Constructor.  
    public Block(String previousHash) {
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        this.hash = calculateHash(); //Making sure we do this after we set the other values.
    }

    //Calculate new hash based on blocks contents
    public String calculateHash() {
        String calculatedhash = StringUtil.applySha256(
                previousHash + 
                Long.toString(timeStamp) + 
                Integer.toString(nonce) + 
                merkleRoot + 
                Long.toString(mineStartTimeStamp)
        );
        return calculatedhash;
    }

    //Increases nonce value until hash target is reached.
    public void mineBlock(int difficulty) {
        mineStartTimeStamp = new Date().getTime(); //added
        merkleRoot = StringUtil.getMerkleRoot(transactions);
        String target = StringUtil.getDificultyString(difficulty, merkleRoot); //edited //Create a string with difficulty * "0" 
        while (!hash.substring(0, difficulty).equals(target)) {
            nonce++;
            hash = calculateHash();
        }
        mineEndTimeStamp = new Date().getTime(); //added
        dificultyString = target; //added
        mined = true;
        System.out.println("Block Mined!!! " + (mineEndTimeStamp - mineStartTimeStamp) + " ms: " + hash); //edited
    }

    //Add transactions to this block
    public boolean addTransaction(Transaction transaction) {
        //process transaction and check if valid, unless block is genesis block then ignore.
        if (transaction == null) {
            return false;
        }
        if ((!"0".equals(previousHash)) && !transaction.asReward) {
            if ((transaction.processTransaction() != true)) {
                System.out.println("Transaction failed to process. Discarded.");
                return false;
            }
        }

        transactions.add(transaction);
        System.out.println("Transaction Successfully added to Block");
        return true;
    }

}
