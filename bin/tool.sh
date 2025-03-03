#!/bin/bash

# 定义路径
SRC_DIR="src"
OUT_DIR="out"
TARGET_FILE="src/com/craftinginterpreters/tool/GenerateAst.java"
RUN_CLASS="com.craftinginterpreters.tool.GenerateAst"
ARGUMENT="src/com/craftinginterpreters/lox"

# 确保输出目录存在
mkdir -p "$OUT_DIR"

# 编译 Java 文件
javac -d "$OUT_DIR" -encoding utf8 -sourcepath "$SRC_DIR" "$TARGET_FILE"

# 检查编译是否成功
if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    
    # 运行 Java 程序
    java -cp "$OUT_DIR" "$RUN_CLASS" "$ARGUMENT"

    if [ $? -eq 0 ]; then
        echo "Execution successful!"
    else
        echo "Execution failed!"
        exit 1
    fi
else
    echo "Compilation failed!"
    exit 1
fi

