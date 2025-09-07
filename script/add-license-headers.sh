#!/bin/bash
# 为所有Java文件添加Apache许可证头

LICENSE_HEADER='/*
 * Copyright 2025 james-zou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'

# 查找所有Java文件并添加许可证头
find src -name "*.java" -type f | while read file; do
    # 检查文件是否已经有许可证头
    if ! grep -q "Copyright 2025 james-zou" "$file"; then
        # 创建临时文件
        temp_file=$(mktemp)
        
        # 添加许可证头
        echo "$LICENSE_HEADER" > "$temp_file"
        
        # 添加原文件内容
        cat "$file" >> "$temp_file"
        
        # 替换原文件
        mv "$temp_file" "$file"
        
        echo "Added license header to: $file"
    else
        echo "License header already exists in: $file"
    fi
done

echo "License headers added to all Java files."
