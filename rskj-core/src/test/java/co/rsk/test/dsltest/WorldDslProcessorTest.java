/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.test.dsltest;

import co.rsk.bitcoinj.core.Address;
import co.rsk.db.RepositorySnapshot;
import co.rsk.test.World;
import co.rsk.test.dsl.DslParser;
import co.rsk.test.dsl.DslProcessorException;
import co.rsk.test.dsl.WorldDslProcessor;
import org.ethereum.core.Account;
import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

/**
 * Created by ajlopez on 8/7/2016.
 */
public class WorldDslProcessorTest {
    @Test
    public void createProcessorWithWorld() {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        Assert.assertSame(world, processor.getWorld());
    }

    @Test
    public void processBlockChainCommandWithOneChildBlock() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("block_chain g00 b01");

        processor.processCommands(parser);

        Block genesis = world.getBlockByName("g00");
        Block block = world.getBlockByName("b01");

        Assert.assertNotNull(block);
        Assert.assertEquals(1, block.getNumber());
        Assert.assertEquals(genesis.getHash(), block.getParentHash());
    }

    @Test
    public void processBlockChainCommandWithTwoChildBlocks() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("block_chain g00 b01 b02");

        processor.processCommands(parser);

        Block genesis = world.getBlockByName("g00");

        Block block1 = world.getBlockByName("b01");

        Assert.assertNotNull(block1);
        Assert.assertEquals(1, block1.getNumber());
        Assert.assertEquals(genesis.getHash(), block1.getParentHash());

        Block block2 = world.getBlockByName("b02");

        Assert.assertNotNull(block2);
        Assert.assertEquals(2, block2.getNumber());
        Assert.assertEquals(block1.getHash(), block2.getParentHash());
    }

    @Test
    public void processBlockChainCommandWithTwoChildBlocksSkippingMultilineComments() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("comment\nthis is a comment\nend\nblock_chain g00 b01 b02\ncomment\nthis is another comment\nwith two lines\n  end  \n");

        processor.processCommands(parser);

        Block genesis = world.getBlockByName("g00");

        Block block1 = world.getBlockByName("b01");

        Assert.assertNotNull(block1);
        Assert.assertEquals(1, block1.getNumber());
        Assert.assertEquals(genesis.getHash(), block1.getParentHash());

        Block block2 = world.getBlockByName("b02");

        Assert.assertNotNull(block2);
        Assert.assertEquals(2, block2.getNumber());
        Assert.assertEquals(block1.getHash(), block2.getParentHash());
    }

    @Test
    public void processBlockConnectCommand() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("block_chain g00 b01\nblock_connect b01");

        processor.processCommands(parser);

        Block genesis = world.getBlockByName("g00");
        Block block = world.getBlockChain().getStatus().getBestBlock();

        Assert.assertNotNull(block);
        Assert.assertEquals(1, block.getNumber());
        Assert.assertEquals(genesis.getHash(), block.getParentHash());
    }

    @Test
    public void processBlockConnectCommandWithTwoBlocks() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("block_chain g00 b01 b02\nblock_connect b01 b02");

        processor.processCommands(parser);

        Block parent = world.getBlockByName("b01");
        Block block = world.getBlockChain().getStatus().getBestBlock();

        Assert.assertNotNull(parent);
        Assert.assertNotNull(block);
        Assert.assertEquals(2, block.getNumber());
        Assert.assertEquals(parent.getHash(), block.getParentHash());
    }

    @Test
    public void processAssertBestCommand() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("block_chain g00 b01 b02\nblock_connect b01 b02\nassert_best b02");

        processor.processCommands(parser);

        Block parent = world.getBlockByName("b01");
        Block block = world.getBlockChain().getStatus().getBestBlock();

        Assert.assertNotNull(parent);
        Assert.assertNotNull(block);
        Assert.assertEquals(2, block.getNumber());
        Assert.assertEquals(parent.getHash(), block.getParentHash());
    }

    @Test
    public void failedAssertBestCommand() {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("block_chain g00 b01 b02\nblock_connect b01 b02\nassert_best b01");

        try {
            processor.processCommands(parser);
            Assert.fail();
        }
        catch (DslProcessorException ex) {
            Assert.assertEquals("Expected best block 'b01'", ex.getMessage());
        }
    }

    @Test
    public void processBlockConnectCommandWithTwoBlocksInFork() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("block_chain g00 b01 b02\nblock_chain b01 c02 c03\nblock_connect b01 b02\n" +
                "block_connect c02 c03");

        processor.processCommands(parser);

        Block parent = world.getBlockByName("c02");
        Block block = world.getBlockChain().getStatus().getBestBlock();

        Assert.assertNotNull(parent);
        Assert.assertNotNull(block);
        Assert.assertEquals(3, block.getNumber());
        Assert.assertEquals(parent.getHash(), block.getParentHash());
    }

    @Test
    public void processAssertConnectWithBlockWithoutParent() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("block_chain g00 b01 b02\nblock_connect b02\nassert_connect no_parent");

        processor.processCommands(parser);

        Block block = world.getBlockChain().getStatus().getBestBlock();
        Assert.assertNotNull(block);
        Assert.assertEquals(0, block.getNumber());
    }

    @Test
    public void processAssertConnectWithImportedBestBlock() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("block_chain g00 b01 b02\nblock_connect b01 b02\nassert_connect best");

        processor.processCommands(parser);

        Block block = world.getBlockChain().getStatus().getBestBlock();
        Assert.assertNotNull(block);
        Assert.assertEquals(2, block.getNumber());
    }

    @Test
    public void processAssertConnectWithImportedNotBestBlock() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("block_chain g00 b01 b02\nblock_chain g00 c01\nblock_connect b01 b02\n" +
                "block_connect c01\nassert_connect not_best");

        processor.processCommands(parser);

        Block block = world.getBlockChain().getStatus().getBestBlock();
        Assert.assertNotNull(block);
        Assert.assertEquals(2, block.getNumber());
    }

    @Test
    public void processAssertBalance() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("account_new acc1 1000\nassert_balance acc1 1000\n");

        processor.processCommands(parser);

        Account account = world.getAccountByName("acc1");
        Assert.assertNotNull(account);
    }

    @Test
    public void processAccountNewCommand() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("account_new acc1");

        processor.processCommands(parser);

        Account account = world.getAccountByName("acc1");

        Assert.assertNotNull(account);
        Assert.assertEquals(BigInteger.ZERO, world.getRepository().getBalance(account.getAddress()).asBigInteger());
    }

    @Test
    public void processAccountNewCommandWithBalance() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("account_new acc1 1000000");

        processor.processCommands(parser);

        Account account = world.getAccountByName("acc1");

        Assert.assertNotNull(account);
        RepositorySnapshot repository = world.getRepositoryLocator().snapshotAt(world.getBlockChain().getBestBlock().getHeader());
        Assert.assertEquals(new BigInteger("1000000"), repository.getBalance(account.getAddress()).asBigInteger());
    }

    @Test
    public void processAccountNewCommandWithBalanceAndCode() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("account_new acc1 1000000 01020304");

        processor.processCommands(parser);

        Account account = world.getAccountByName("acc1");

        Assert.assertNotNull(account);
        RepositorySnapshot repository = world.getRepositoryLocator().snapshotAt(world.getBlockChain().getBestBlock().getHeader());
        Assert.assertEquals(new BigInteger("1000000"), repository.getBalance(account.getAddress()).asBigInteger());

        byte[] code = repository.getCode(account.getAddress());

        Assert.assertNotNull(code);
        Assert.assertArrayEquals(new byte[] { 0x01, 0x02, 0x03, 0x04 }, code);
    }

    @Test
    public void processAccountNewCommandWithBalanceAndCodeWithOtherAccountAddress() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser0 = new DslParser("account_new acc0");

        processor.processCommands(parser0);

        DslParser parser = new DslParser("account_new acc1 1000000 0102[acc0]0304");

        processor.processCommands(parser);

        Account account0 = world.getAccountByName("acc0");
        Account account = world.getAccountByName("acc1");

        Assert.assertNotNull(account);
        RepositorySnapshot repository = world.getRepositoryLocator().snapshotAt(world.getBlockChain().getBestBlock().getHeader());
        Assert.assertEquals(new BigInteger("1000000"), repository.getBalance(account.getAddress()).asBigInteger());

        byte[] code = repository.getCode(account.getAddress());
        byte[] expected = new byte[4 + 20];
        expected[0] = 0x01;
        expected[1] = 0x02;
        System.arraycopy(account0.getAddress().getBytes(), 0, expected, 2, Address.LENGTH);
        expected[22] = 0x03;
        expected[23] = 0x04;

        Assert.assertNotNull(code);
        Assert.assertArrayEquals(expected, code);
    }

    @Test
    public void raiseIfUnknownCommand() {
        WorldDslProcessor processor = new WorldDslProcessor(new World());

        try {
            processor.processCommands(new DslParser("foo"));
            Assert.fail();
        }
        catch (DslProcessorException ex) {
            Assert.assertEquals("Unknown command 'foo'", ex.getMessage());
        }
    }

    @Test
    public void processBlockBuildCommand() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("block_build b01\nparent g00\nbuild");

        processor.processCommands(parser);

        Block block = world.getBlockByName("b01");
        Assert.assertNotNull(block);
        Assert.assertEquals(1, block.getNumber());
    }

    @Test
    public void processBlockBuildCommandWithUncles() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("block_chain g00 b01\nblock_chain g00 u01\nblock_chain g00 u02\nblock_build b02\nparent b01\nuncles u01 u02\nbuild");

        processor.processCommands(parser);

        Block block = world.getBlockByName("b02");
        Assert.assertNotNull(block);
        Assert.assertEquals(2, block.getNumber());
        Assert.assertNotNull(block.getUncleList());
        Assert.assertFalse(block.getUncleList().isEmpty());
        Assert.assertEquals(2, block.getUncleList().size());

        Assert.assertEquals(world.getBlockByName("u01").getHash(), block.getUncleList().get(0).getHash());
        Assert.assertEquals(world.getBlockByName("u02").getHash(), block.getUncleList().get(1).getHash());
    }

    @Test
    public void processTransactionBuildCommand() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("account_new acc1\naccount_new acc2\ntransaction_build tx01\nsender acc1\nreceiver acc2\nvalue 1000\nbuild");

        processor.processCommands(parser);

        Account acc1 = world.getAccountByName("acc1");
        Account acc2 = world.getAccountByName("acc2");

        Assert.assertNotNull(acc1);
        Assert.assertNotNull(acc2);

        Transaction tx01 = world.getTransactionByName("tx01");

        Assert.assertNotNull(tx01);

        Assert.assertArrayEquals(acc1.getAddress().getBytes(), tx01.getSender().getBytes());
        Assert.assertArrayEquals(acc2.getAddress().getBytes(), tx01.getReceiveAddress().getBytes());
        Assert.assertEquals(new BigInteger("1000"), tx01.getValue().asBigInteger());
        Assert.assertNotNull(tx01.getData());
        Assert.assertEquals(0, tx01.getData().length);
    }

    @Test
    public void processTransactionWithDataBuildCommand() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("account_new acc1\naccount_new acc2\ntransaction_build tx01\nsender acc1\nreceiver acc2\nvalue 1000\ndata 01020304\nbuild");

        processor.processCommands(parser);

        Account acc1 = world.getAccountByName("acc1");
        Account acc2 = world.getAccountByName("acc2");

        Assert.assertNotNull(acc1);
        Assert.assertNotNull(acc2);

        Transaction tx01 = world.getTransactionByName("tx01");

        Assert.assertNotNull(tx01);

        Assert.assertArrayEquals(acc1.getAddress().getBytes(), tx01.getSender().getBytes());
        Assert.assertArrayEquals(acc2.getAddress().getBytes(), tx01.getReceiveAddress().getBytes());
        Assert.assertEquals(new BigInteger("1000"), tx01.getValue().asBigInteger());
        Assert.assertNotNull(tx01.getData());
        Assert.assertArrayEquals(new byte[] { 0x01, 0x02, 0x03, 0x04 }, tx01.getData());
    }

    @Test
    public void processTransactionWithGasAndGasPriceBuildCommand() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("account_new acc1\naccount_new acc2\ntransaction_build tx01\nsender acc1\nreceiver acc2\nvalue 1000\ngas 1200000\ngasPrice 2\nbuild");

        processor.processCommands(parser);

        Account acc1 = world.getAccountByName("acc1");
        Account acc2 = world.getAccountByName("acc2");

        Assert.assertNotNull(acc1);
        Assert.assertNotNull(acc2);

        Transaction tx01 = world.getTransactionByName("tx01");

        Assert.assertNotNull(tx01);

        Assert.assertArrayEquals(acc1.getAddress().getBytes(), tx01.getSender().getBytes());
        Assert.assertArrayEquals(acc2.getAddress().getBytes(), tx01.getReceiveAddress().getBytes());
        Assert.assertEquals(new BigInteger("1000"), tx01.getValue().asBigInteger());
        Assert.assertNotNull(tx01.getData());
        Assert.assertEquals(0, tx01.getData().length);
        Assert.assertEquals(new BigInteger("2"), tx01.getGasPrice().asBigInteger());
        Assert.assertEquals(new BigInteger("1200000"), tx01.getGasLimitAsInteger());
    }

    @Test
    public void processTransactionWithNonceBuildCommand() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("account_new acc1\naccount_new acc2\ntransaction_build tx01\nsender acc1\nreceiver acc2\nvalue 1000\nnonce 10\nbuild");

        processor.processCommands(parser);

        Account acc1 = world.getAccountByName("acc1");
        Account acc2 = world.getAccountByName("acc2");

        Assert.assertNotNull(acc1);
        Assert.assertNotNull(acc2);

        Transaction tx01 = world.getTransactionByName("tx01");

        Assert.assertNotNull(tx01);

        Assert.assertArrayEquals(acc1.getAddress().getBytes(), tx01.getSender().getBytes());
        Assert.assertArrayEquals(acc2.getAddress().getBytes(), tx01.getReceiveAddress().getBytes());
        Assert.assertEquals(new BigInteger("1000"), tx01.getValue().asBigInteger());
        Assert.assertNotNull(tx01.getData());
        Assert.assertEquals(0, tx01.getData().length);
        Assert.assertEquals(new BigInteger("10"), tx01.getNonceAsInteger());
    }

    @Test
    public void processBlockBuildCommandWithTransactions() throws DslProcessorException {
        World world = new World();

        WorldDslProcessor processor = new WorldDslProcessor(world);

        DslParser parser = new DslParser("account_new acc1\naccount_new acc2\n" +
                "transaction_build tx01\nsender acc1\nreceiver acc2\nvalue 1000\nbuild\n" +
                "transaction_build tx02\nsender acc1\nreceiver acc2\nvalue 1000\nbuild\n" +
                "block_build b01\nparent g00\ntransactions tx01 tx02\nbuild\n");

        processor.processCommands(parser);

        Block block = world.getBlockByName("b01");
        Assert.assertNotNull(block);
        Assert.assertEquals(1, block.getNumber());
        Assert.assertNotNull(block.getTransactionsList());
        Assert.assertFalse(block.getTransactionsList().isEmpty());
        Assert.assertEquals(2, block.getTransactionsList().size());

        Transaction tx01 = world.getTransactionByName("tx01");
        Transaction tx02 = world.getTransactionByName("tx02");

        Assert.assertNotNull(tx01);
        Assert.assertNotNull(tx02);

        Assert.assertEquals(tx01.getHash(), block.getTransactionsList().get(0).getHash());
        Assert.assertEquals(tx02.getHash(), block.getTransactionsList().get(1).getHash());
    }
}
