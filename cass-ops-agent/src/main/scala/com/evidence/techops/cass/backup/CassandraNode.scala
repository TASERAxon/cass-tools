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

package com.evidence.techops.cass.backup

import com.evidence.techops.cass.agent.config.ServiceConfig
import com.evidence.techops.cass.persistence.LocalDB

/**
 * Created by pmahendra on 10/7/14.
 */
class CassandraNode(config: ServiceConfig, servicePersistence: LocalDB) extends BackupBase(config, servicePersistence)

object CassandraNode {
  def apply(config:ServiceConfig, servicePersistence: LocalDB) = {
    new CassandraNode(config, servicePersistence)
  }
}
