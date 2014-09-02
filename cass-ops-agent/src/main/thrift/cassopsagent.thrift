/**
 * Copyright 2015 TASER International, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

namespace cpp com.evidence.techops.cass
namespace d com.evidence.techops.cass
namespace java com.evidence.techops.cass
namespace php com.evidence.techops.cass
namespace perl com.evidence.techops.cass
namespace csharp com.evidence.techops.cass
namespace go com.evidence.techops.cass
namespace py com.evidence.techops.cass

exception BackupRestoreException
{
  1: optional i64 code,
  2: optional string message
}

service CassOpsAgent
{
    string getStatus(),
    string getColumnFamilyMetric(1: string keySpace, 2: string colFamily),

    string incrementalBackup(1: string keySpace) throws (1: BackupRestoreException ea),
    string snapshotBackup(1: string keySpace) throws (1: BackupRestoreException ea),            // snapshot directory to s3
    string snapshotBackup2(1: string keySpace) throws (1: BackupRestoreException ea),           // snapshot directory -> backup directory + gzip -> s3
    string commitLogBackup() throws (1: BackupRestoreException ea),

    // host id can be null. defaults to current node host id if null
    void restoreBackup(1: string keySpace, 2: string snapShotName, 3: string hostId) throws (1: BackupRestoreException ea),

    string csvToSsTableConv(1: string csvFilePath, 2: string keySpace, 3: string colFamily, 4: string partitioner) throws (1: BackupRestoreException ea),
    bool ssTableImport(1: string ssTableFilePath, 2: string keySpace, 3: string colFamily) throws (1: BackupRestoreException ea),
}