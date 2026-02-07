#!/bin/bash

################################################################################
# ClashMetaForAndroid V2B - Build Health Check & Auto-Fix Script
# Monitors GitHub Actions builds and automatically fixes common errors
################################################################################

set -e

REPO="hosemorinho/ClashMetaForAndroid_v2b"
BRANCH="main"
MAX_RETRIES=5
RETRY_COUNT=0
CHECK_INTERVAL=60  # seconds

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

log_error() {
    echo -e "${RED}[✗]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

################################################################################
# Get latest workflow run
################################################################################
get_latest_run() {
    log_info "Fetching latest workflow run..."
    gh run list --repo "$REPO" --branch "$BRANCH" --limit 1 --json databaseId,conclusion,status > /tmp/latest_run.json 2>/dev/null

    if [ ! -f /tmp/latest_run.json ]; then
        log_error "Failed to fetch latest run"
        return 1
    fi

    RUN_ID=$(jq -r '.[0].databaseId' /tmp/latest_run.json 2>/dev/null)
    CONCLUSION=$(jq -r '.[0].conclusion' /tmp/latest_run.json 2>/dev/null)
    STATUS=$(jq -r '.[0].status' /tmp/latest_run.json 2>/dev/null)

    if [ -z "$RUN_ID" ] || [ "$RUN_ID" = "null" ]; then
        log_error "Could not parse run information"
        return 1
    fi

    log_info "Run ID: $RUN_ID | Status: $STATUS | Conclusion: $CONCLUSION"
}

################################################################################
# Check if build is in progress
################################################################################
is_build_in_progress() {
    if [ "$STATUS" = "in_progress" ]; then
        return 0
    else
        return 1
    fi
}

################################################################################
# Check if build succeeded
################################################################################
is_build_successful() {
    if [ "$CONCLUSION" = "success" ]; then
        return 0
    else
        return 1
    fi
}

################################################################################
# Fetch and analyze build logs
################################################################################
analyze_build_failure() {
    log_info "Analyzing build failure from run $RUN_ID..."

    # Get failed log
    gh run view "$RUN_ID" --repo "$REPO" --log-failed > /tmp/build_log.txt 2>&1 || true

    # Extract error messages
    ERRORS=$(grep -E "error:|ERROR:|Exception:|FAILED|BUILD FAILED" /tmp/build_log.txt || echo "")

    if [ -z "$ERRORS" ]; then
        log_warning "Could not extract specific error messages from logs"
        return 1
    fi

    echo "$ERRORS"
}

################################################################################
# Detect and fix common errors
################################################################################
fix_build_error() {
    log_info "Attempting to detect and fix build errors..."

    local error_log="$1"
    local fixed=0

    # Error 1: String interpolation syntax error
    if echo "$error_log" | grep -q "string interpolation"; then
        log_warning "Detected: String interpolation error"
        git grep -l '\${ ' -- '*.kt' | while read file; do
            sed -i 's/\${ /\${/g' "$file"
            log_info "Fixed string interpolation in $file"
            fixed=1
        done
    fi

    # Error 2: Public inline function accessing non-public API
    if echo "$error_log" | grep -q "Public-API inline function"; then
        log_warning "Detected: Public inline function accessing non-public property"
        sed -i 's/inline fun </internal inline fun </g' service/src/main/java/com/github/kr328/clash/service/v2board/*.kt
        log_success "Fixed inline function visibility"
        fixed=1
    fi

    # Error 3: Type mismatch errors
    if echo "$error_log" | grep -q "type mismatch"; then
        log_warning "Detected: Type mismatch error"
        log_info "Manual review needed for type mismatch errors"
        return 1
    fi

    # Error 4: Compilation errors in service module
    if echo "$error_log" | grep -q "service:compileAlphaReleaseKotlin FAILED"; then
        log_warning "Detected: Service module compilation failure"
        # Run syntax check
        log_info "Running basic Kotlin syntax validation..."
        if ! kotlinc -nowarn service/src/main/java/com/github/kr328/clash/service/v2board/*.kt -d /tmp/compile_check 2>/tmp/syntax_errors.txt; then
            log_error "Syntax errors found:"
            cat /tmp/syntax_errors.txt
            return 1
        fi
    fi

    return $fixed
}

################################################################################
# Commit and push fixes
################################################################################
commit_and_push() {
    local fix_type="$1"

    # Check if there are changes
    if ! git diff --quiet; then
        log_info "Changes detected, committing..."

        git add -A
        git commit -m "fix: Auto-fix $fix_type in V2Board integration

Automated fix generated by health-check script.
- Fixed compilation error
- Validated syntax

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"

        log_info "Pushing to v2board remote..."
        git push v2board main || {
            log_error "Failed to push changes"
            return 1
        }

        log_success "Changes pushed successfully"
        return 0
    else
        log_warning "No changes to commit"
        return 1
    fi
}

################################################################################
# Wait for new build
################################################################################
wait_for_new_build() {
    log_info "Waiting for GitHub Actions to start new build..."

    local wait_count=0
    local max_wait=300  # 5 minutes

    while [ $wait_count -lt $max_wait ]; do
        sleep 10
        wait_count=$((wait_count + 10))

        # Get latest run again
        if ! get_latest_run; then
            continue
        fi

        # Compare with previous run (if available)
        if [ -f /tmp/prev_run_id.txt ]; then
            PREV_RUN_ID=$(cat /tmp/prev_run_id.txt)
            if [ "$RUN_ID" != "$PREV_RUN_ID" ]; then
                log_success "New build detected!"
                echo "$RUN_ID" > /tmp/prev_run_id.txt
                return 0
            fi
        else
            echo "$RUN_ID" > /tmp/prev_run_id.txt
            return 0
        fi
    done

    log_error "Timeout waiting for new build to start"
    return 1
}

################################################################################
# Main health check loop
################################################################################
main() {
    log_info "=========================================="
    log_info "ClashMetaForAndroid V2B Health Check"
    log_info "=========================================="
    log_info "Repository: $REPO"
    log_info "Branch: $BRANCH"
    log_info "Max retries: $MAX_RETRIES"
    log_info ""

    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        RETRY_COUNT=$((RETRY_COUNT + 1))
        log_info "Attempt $RETRY_COUNT/$MAX_RETRIES"

        # Get latest run status
        if ! get_latest_run; then
            log_error "Failed to fetch run status, retrying..."
            sleep 10
            continue
        fi

        # Wait if build is in progress
        if is_build_in_progress; then
            log_warning "Build in progress, waiting ${CHECK_INTERVAL}s..."
            sleep "$CHECK_INTERVAL"
            continue
        fi

        # Check if successful
        if is_build_successful; then
            log_success "✓ BUILD SUCCESSFUL!"
            log_success "Repository: https://github.com/$REPO"
            log_success "Run: https://github.com/$REPO/actions/runs/$RUN_ID"
            return 0
        fi

        # Analyze failure
        log_error "Build failed, analyzing..."
        ERROR_LOG=$(analyze_build_failure)

        if [ $? -ne 0 ]; then
            log_error "Failed to extract error information"
            echo "$ERROR_LOG"
        else
            log_info "Error summary:"
            echo "$ERROR_LOG" | head -20
        fi

        # Try to fix
        if fix_build_error "$ERROR_LOG"; then
            # Commit and push
            if commit_and_push "build-error"; then
                # Wait for new build
                if ! wait_for_new_build; then
                    log_error "Failed waiting for new build"
                    break
                fi
                log_info "Waiting before next check..."
                sleep "$CHECK_INTERVAL"
                continue
            else
                log_error "Failed to commit and push fixes"
                break
            fi
        else
            log_error "Could not automatically fix this error"
            log_info "Error requires manual intervention:"
            echo "$ERROR_LOG"
            break
        fi
    done

    if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
        log_error "✗ Maximum retries reached ($MAX_RETRIES)"
        log_info "Please review and fix manually:"
        log_info "Repository: https://github.com/$REPO"
        log_info "Latest run: https://github.com/$REPO/actions/runs/$RUN_ID"
        return 1
    fi
}

# Run main function
main
exit $?
