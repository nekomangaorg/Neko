
## 2024-03-12 - Wrap skydoves ApiResponse properly with Result
**Learning:** When refactoring to `michaelbull.result` and dealing with the `skydoves.sandwich.ApiResponse`, you should not use `runCatching { apiCall() }` because Sandwich internally catches exceptions and returns them as `ApiResponse.Failure.Exception`. Instead, manually check for `ApiResponse.Failure` and explicitly map `ApiResponse.Failure.Error` to `ResultError.HttpError(statusCode)` and `ApiResponse.Failure.Exception` to `ResultError.Generic(message)` to accurately differentiate between "No Internet" (IOException inside Exception) and "Server Down" (HttpError).
**Action:** Always map the sub-types of `ApiResponse.Failure` explicitly when adapting Sandwich API methods to Result error patterns.
