#
# If not stated otherwise in this file or this component's LICENSE file the
# following copyright and licenses apply:
#
# Copyright 2020 Liberty Global B.V.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Author: Damian Wrobel <dwrobel@ertelnet.rybnik.pl>
#

ONEMW_CACHE_NAME ?= "${BPN}"
ONEMW_CACHE_VERSION ?= "${PV}"
ONEMW_CACHE_ROOT ?= "${DL_DIR}/onemw-cache"
ONEMW_CACHE_LOCAL_PATH ?= "${ONEMW_CACHE_ROOT}/${ONEMW_CACHE_NAME}-${ONEMW_CACHE_VERSION}"
ONEMW_CACHE_LOCAL_PATH_LOCK ?= "${ONEMW_CACHE_LOCAL_PATH}.lock"

# Can be used to implement a functionality to store/retrieve data from e.g. S3 Object Store.
