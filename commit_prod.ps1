# Professional Commit Script for Spring Chat Project
# This script bypasses the node-pty issue and ensures a production-ready commit

Write-Host "--- Preparing Production-Ready Commit ---" -ForegroundColor Cyan

# Stage all changes
git add .

# Professional commit message
$commitMsg = @"
feat(friendship): implement reject friend request functionality

- Add POST /api/friendships/requests/{id}/reject endpoint
- Add rejectFriendShip logic in service with business rule validation
- Ensure only the addressee can reject a pending request
- Add RejectFriendResponseDTO for consistent API response
- Add comprehensive Unit Tests for rejectFriendShip logic
"@

git commit -m "$commitMsg"

Write-Host "`n--- Success! ---" -ForegroundColor Green
git status
