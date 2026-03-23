#!/bin/bash

set -euo pipefail  # ✅ Added -u (undefined vars) and -o pipefail

# Resolve the android project root relative to this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$ANDROID_DIR"

# Configuration
MAX_RETRIES=3
DELAY_BETWEEN_COMMANDS=3
RETRY_DELAY=1
GRADLE_WRAPPER="./gradlew"
MODULES=()

# ✅ Color output for better readability
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ✅ Discover modules dynamically by finding those with a generateBaselineProfile task
discover_modules() {
    echo -e "${BLUE}🔍 Discovering modules with baseline profile support...${NC}"
    local tasks_output
    tasks_output=$("$GRADLE_WRAPPER" tasks --all 2>&1 | grep ":generateBaselineProfile " | sed 's/:generateBaselineProfile .*//' | sed 's/^://' | sort -u || true)

    if [ -z "$tasks_output" ]; then
        echo -e "${RED}❌ No modules found with generateBaselineProfile task${NC}"
        exit 1
    fi

    MODULES=()
    while IFS= read -r module; do
        MODULES+=("$module")
    done <<< "$tasks_output"

    echo -e "${GREEN}✅ Found ${#MODULES[@]} module(s): ${MODULES[*]}${NC}"
    echo ""
}

# ✅ Track timing
START_TIME=$(date +%s)

# ✅ Cleanup function
cleanup() {
    local exit_code=$?
    if [ $exit_code -ne 0 ]; then
        echo -e "${RED}❌ Script failed with exit code $exit_code${NC}"
        echo -e "${YELLOW}💡 Check logs above for details${NC}"
    fi

    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    echo -e "${BLUE}⏱️  Total time: ${DURATION}s${NC}"
}

trap cleanup EXIT

# ✅ Validation function
validate_prerequisites() {
    echo -e "${BLUE}🔍 Validating prerequisites...${NC}"

    # Check if gradlew exists
    if [ ! -f "$GRADLE_WRAPPER" ]; then
        echo -e "${RED}❌ Gradle wrapper not found: $GRADLE_WRAPPER${NC}"
        exit 1
    fi

    # Check if gradlew is executable
    if [ ! -x "$GRADLE_WRAPPER" ]; then
        echo -e "${YELLOW}⚠️  Making gradlew executable...${NC}"
        chmod +x "$GRADLE_WRAPPER"
    fi

    # Check for connected devices
    #if ! adb devices | grep -q "device$"; then
    #   echo -e "${RED}❌ No Android device connected${NC}"
    #   echo -e "${YELLOW}💡 Connect device and enable USB debugging${NC}"
    #   exit 1
    #fi

    # Check Gradle daemon status
    "$GRADLE_WRAPPER" --status || true

    echo -e "${GREEN}✅ Prerequisites validated${NC}"
    echo ""
}

# ✅ Improved retry function with better error handling
run_with_retry() {
    local cmd="$1"
    local module_name="$2"
    local max_attempts=$MAX_RETRIES
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        echo -e "${BLUE}📦 [$module_name] Attempt $attempt/$max_attempts${NC}"

        # ✅ Capture both stdout and stderr
        if eval "$cmd" 2>&1 | tee "/tmp/baseline_${module_name}_${attempt}.log"; then
            echo -e "${GREEN}✅ [$module_name] Success${NC}"
            return 0
        else
            local exit_code=$?
            echo -e "${RED}❌ [$module_name] Failed with exit code $exit_code (attempt $attempt/$max_attempts)${NC}"
            echo -e "${YELLOW}📋 Log saved: /tmp/baseline_${module_name}_${attempt}.log${NC}"

            if [ $attempt -lt $max_attempts ]; then
                echo -e "${YELLOW}⏳ Waiting ${RETRY_DELAY}s before retry...${NC}"
                sleep $RETRY_DELAY

                # ✅ Stop Gradle daemon between retries to avoid conflicts
                echo -e "${YELLOW}🔄 Stopping Gradle daemon...${NC}"
                "$GRADLE_WRAPPER" --stop || true
                sleep 2
            fi

            attempt=$((attempt + 1))
        fi
    done

    echo -e "${RED}💥 [$module_name] Failed after $max_attempts attempts${NC}"
    return 1
}

# ✅ Function to add delay between commands
delay_between_commands() {
    if [ -n "${1:-}" ]; then
        echo -e "${BLUE}⏱️  Module complete. Waiting ${DELAY_BETWEEN_COMMANDS}s before next...${NC}"
        sleep $DELAY_BETWEEN_COMMANDS
    fi
}

# ✅ Check if baseline profiles have uncommitted changes
check_git_status() {
    if git diff --quiet '**/baseline-prof.txt'; then
        echo -e "${GREEN}✅ No previous uncommitted baseline profile changes${NC}"
    else
        echo -e "${YELLOW}⚠️  Uncommitted changes detected in baseline profiles${NC}"
        echo -e "${YELLOW}💡 Continuing anyway...${NC}"
    fi
    echo ""
}

# ✅ Main execution
main() {
    echo -e "${BLUE}🚀 Starting baseline profile generation...${NC}"
    echo -e "${BLUE}Configuration:${NC}"
    echo -e "  MAX_RETRIES: $MAX_RETRIES"
    echo -e "  DELAY: ${DELAY_BETWEEN_COMMANDS}s"
    echo -e "  MODULES: ${#MODULES[@]}"
    echo ""

    validate_prerequisites
    discover_modules
    check_git_status

    local total_modules=${#MODULES[@]}
    local current=0

    for module in "${MODULES[@]}"; do
        current=$((current + 1))

        echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        echo -e "${BLUE}📦 Module $current/$total_modules: $module${NC}"
        echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

        local cmd="$GRADLE_WRAPPER :$module:generateBaselineProfile -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile"

        # ✅ Run with retry (no need for || exit 1 with set -e)
        run_with_retry "$cmd" "$module"

        # ✅ Only delay if not the last module
        if [ $current -lt $total_modules ]; then
            delay_between_commands "yes"
        fi

        echo ""
    done

    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}🎉 All baseline profiles generated successfully!${NC}"
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

    # ✅ Show git status
    echo ""
    echo -e "${BLUE}📊 Git status:${NC}"
    if git diff --quiet '**/baseline-prof.txt'; then
        echo -e "${YELLOW}⚠️  No changes detected in baseline profiles${NC}"
        echo -e "${YELLOW}💡 This might indicate:${NC}"
        echo -e "${YELLOW}   - Profiles are already up to date${NC}"
        echo -e "${YELLOW}   - Generation didn't complete successfully${NC}"
    else
        echo -e "${GREEN}✅ Baseline profiles updated:${NC}"
        git diff --stat '**/baseline-prof.txt'
        echo ""
        echo -e "${YELLOW}💡 Next steps:${NC}"
        echo -e "${YELLOW}   git add '**/baseline-prof.txt'${NC}"
        echo -e "${YELLOW}   git commit -m 'chore: update baseline profiles'${NC}"
    fi
}

# ✅ Run main function
main "$@"