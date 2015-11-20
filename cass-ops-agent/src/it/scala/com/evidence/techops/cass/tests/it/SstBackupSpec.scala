package com.evidence.techops.cass.tests.it

import java.io.File
import java.util.UUID

import com.datastax.driver.core.{BoundStatement, Cluster, Session}
import com.evidence.techops.cass.agent.ServiceGlobal
import com.evidence.techops.cass.agent.config.ServiceConfig
import com.evidence.techops.cass.backup.BackupType._
import com.evidence.techops.cass.backup.{IncrementalBackup, SnapshotBackup}
import com.evidence.techops.cass.backup.storage.Compress
import com.evidence.techops.cass.client.Cassandra
import com.evidence.techops.cass.persistence.LocalDB
import com.evidence.techops.cass.restore.{RestoreBackup, RestoredBackup}
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.FileUtils
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

/**
  * Created by pmahendra on 11/20/15.
  */

class SstBackupSpec extends FlatSpec with Matchers with LazyLogging with BeforeAndAfterAll {
   var serviceConfig: ServiceConfig = null
   var cassandraCluster: Cluster = null
   var cassandraSession: Session = null
   var sstTool: IncrementalBackup = null
   var restoreTool: RestoreBackup = null
   var servicePersistence: LocalDB = null

   val testKeyspaceName = "cassops_it_tests"
   val createTestKeysapceIfNotExists = s"CREATE KEYSPACE IF NOT EXISTS $testKeyspaceName WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'}  AND durable_writes = true;"
   val createTestCf =
     s"""
       |CREATE TABLE IF NOT EXISTS $testKeyspaceName.test_cf (
       |   test_key text,
       |   primary key (test_key)
       |);
     """.stripMargin

   val insertTestData = s"INSERT INTO $testKeyspaceName.test_cf (test_key) VALUES (?)"

   override def beforeAll(): Unit = {
     ServiceGlobal.init()

     serviceConfig = ServiceConfig.load()
     cassandraCluster = Cassandra.connect(serviceConfig, false)
     cassandraSession = cassandraCluster.connect()
     servicePersistence = LocalDB.apply(serviceConfig, "it-test-service-persistence")
     sstTool = IncrementalBackup.apply(serviceConfig, servicePersistence)
     restoreTool = RestoreBackup.apply(serviceConfig, servicePersistence)
   }

  it should "create a sst backup compressed to s3 and restore" in {
    val createTestKeysapceIfNotExistsPrepped = cassandraSession.prepare(createTestKeysapceIfNotExists)
    val createTestCfPrepped = cassandraSession.prepare(createTestCf)
    val insertTestDataPrepped = cassandraSession.prepare(insertTestData)

    // create test keyspace
    cassandraSession.execute(new BoundStatement(createTestKeysapceIfNotExistsPrepped))

    // create test cf
    cassandraSession.execute(new BoundStatement(createTestCfPrepped))

    // insert test data
    var testData = UUID.randomUUID().toString
    cassandraSession.execute(new BoundStatement(insertTestDataPrepped).bind(testData))

    testData = UUID.randomUUID().toString
    cassandraSession.execute(new BoundStatement(insertTestDataPrepped).bind(testData))

    testData = UUID.randomUUID().toString
    cassandraSession.execute(new BoundStatement(insertTestDataPrepped).bind(testData))

    // clear all snapshots
    sstTool.forceKeySpaceFlush(testKeyspaceName)


    val snapshotName = "it-test-snapshot-" + DateTime.now().toString(ISODateTimeFormat.basicDateTime())

    // verify the keyspace data folder
    val keySpaceDataDir = sstTool.getKeySpaceDataDirectory(testKeyspaceName)
    assert(keySpaceDataDir.isDefined && keySpaceDataDir.get.isDirectory)
    assert(keySpaceDataDir.get.getAbsolutePath.startsWith(serviceConfig.getCassDataFileDir()))
    logger.info(s"keyspace data dir: ${keySpaceDataDir.get.getAbsolutePath}")

    // verify the sst directory list
    val sstDirList = sstTool.getKeySpaceSstDirectoryList(testKeyspaceName)

    assert(sstDirList.isDefined)
    assert(sstDirList.get.length > 0)

    // upload sst
    val filesBackedupCount = sstTool.uploadSst(testKeyspaceName, snapshotName, isCompressed = true)
    assert(filesBackedupCount > 0)

    // download file and verify
    FileUtils.deleteDirectory(new File(serviceConfig.getRestoreLocalDir()))
    val filesRestored = restoreTool.restoreBackupFiles(SST, testKeyspaceName, snapshotName, restoreTool.getLocalHostId)
    assert(filesRestored != null && filesRestored.size > 0)

    // untar files
    val destFolder = new File(serviceConfig.getRestoreLocalDir(), "untarred")
    destFolder.mkdirs()

    filesRestored.foreach((restoredBackup: RestoredBackup) => {
      Compress.extractFiles(restoredBackup.localFile, destFolder)
    })

    val restoredFilesCount = destFolder.listFiles().length
    logger.info(s"restoredFilesCount = $restoredFilesCount filesBackedupCount = $filesBackedupCount")
    assert(restoredFilesCount == filesBackedupCount) // -1 for the backup manifest
  }

   it should "create a sst backup to s3 and restore" in {
     val createTestKeysapceIfNotExistsPrepped = cassandraSession.prepare(createTestKeysapceIfNotExists)
     val createTestCfPrepped = cassandraSession.prepare(createTestCf)
     val insertTestDataPrepped = cassandraSession.prepare(insertTestData)

     // create test keyspace
     cassandraSession.execute(new BoundStatement(createTestKeysapceIfNotExistsPrepped))

     // create test cf
     cassandraSession.execute(new BoundStatement(createTestCfPrepped))

     // insert test data
     var testData = UUID.randomUUID().toString
     cassandraSession.execute(new BoundStatement(insertTestDataPrepped).bind(testData))

     testData = UUID.randomUUID().toString
     cassandraSession.execute(new BoundStatement(insertTestDataPrepped).bind(testData))

     testData = UUID.randomUUID().toString
     cassandraSession.execute(new BoundStatement(insertTestDataPrepped).bind(testData))

     // clear all snapshots
     sstTool.forceKeySpaceFlush(testKeyspaceName)


     val snapshotName = "it-test-snapshot-" + DateTime.now().toString(ISODateTimeFormat.basicDateTime())

     // verify the keyspace data folder
     val keySpaceDataDir = sstTool.getKeySpaceDataDirectory(testKeyspaceName)
     assert(keySpaceDataDir.isDefined && keySpaceDataDir.get.isDirectory)
     assert(keySpaceDataDir.get.getAbsolutePath.startsWith(serviceConfig.getCassDataFileDir()))
     logger.info(s"keyspace data dir: ${keySpaceDataDir.get.getAbsolutePath}")

     // verify the sst directory list
     val sstDirList = sstTool.getKeySpaceSstDirectoryList(testKeyspaceName)

     assert(sstDirList.isDefined)
     assert(sstDirList.get.length > 0)

     // upload sst
     val filesBackedupCount = sstTool.uploadSst(testKeyspaceName, snapshotName, isCompressed = false)
     assert(filesBackedupCount > 0)

     logger.info(s"filesBackedupCount = $filesBackedupCount")
   }
 }
