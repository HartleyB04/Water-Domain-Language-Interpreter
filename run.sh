#!/bin/bash
# Run script for River Flow Interpreter

if [ $# -eq 0 ]; then
    echo "Usage: ./run.sh <program.lox>"
    echo ""
    echo "Examples:"
    echo "To run Demo Programs:"
    echo "  java -jar build/lox.jar 'Demo Programs'/program_1.lox"
    echo "  java -jar build/lox.jar 'Demo Programs'/program_2.lox"
    echo "  java -jar build/lox.jar 'Demo Programs'/program_3.lox"
    echo "  java -jar build/lox.jar 'Demo Programs'/program_4.lox"
    exit 1
fi

# Check if JAR exists
if [ ! -f build/lox.jar ]; then
    echo "Error: build/lox.jar not found. Please run ./compile.sh first."
    exit 1
fi

# Run the program
java -jar build/lox.jar "$1"