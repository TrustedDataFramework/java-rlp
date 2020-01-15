package org.tdf.rlp;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Block {
    private static final String PREFIX = "blocks-dump";

    public long nVersion;

    // parent block hash
    public byte[] hashPrevBlock;

    // merkle root of transactions
    public byte[] hashMerkleRoot;

    public byte[] hashMerkleState;

    public byte[] hashMerkleIncubate;

    // 32bit unsigned block number
    public long nHeight;

    // 32bit unsigned unix epoch
    public long nTime;

    public byte[] nBits;

    // random value from proposer 256bit, next block's seed
    public byte[] nNonce;

    @RLPIgnored
    public byte[] blockNotice;

    public List<Transaction> body;

    public static void main(String[] args) {
        try{
            eval();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void eval() {
        String directory = System.getenv("BLOCKS_DIRECTORY");
        File file = Paths.get(directory).toFile();
        Stream<byte[]> data = Optional.of(file)
                .filter(File::isDirectory)
                .map(File::listFiles)
                .map(Arrays::stream)
                .orElse(Stream.empty())
                .filter(f -> !f.isDirectory() && f.getName().startsWith(PREFIX))
                .sorted(Comparator.comparingInt(x -> Integer.parseInt(x.getName().split("\\.")[1])))
                .map(x -> {
                    try {
                        return Files.readAllBytes(x.toPath());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        long start = System.currentTimeMillis();
        long op = data
                .map(binary -> RLPCodec.decode(binary, Block[].class))
                .mapToLong(blocks -> blocks.length)
                .sum();

        long end = System.currentTimeMillis();
        System.out.println(
                String.format("decode %d times, total: %d ms, average %.3f ms",
                        op,
                        end -start,
                        Double.valueOf(end - start) / op
                ));
    }
}
