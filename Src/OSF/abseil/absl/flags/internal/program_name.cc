//
//  Copyright 2019 The Abseil Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      https://www.apache.org/licenses/LICENSE-2.0
//
#include "absl/absl-internal.h"
#pragma hdrstop

namespace absl {
ABSL_NAMESPACE_BEGIN
namespace flags_internal {
ABSL_CONST_INIT static absl::Mutex program_name_guard(absl::kConstInit);
ABSL_CONST_INIT static std::string* program_name
    ABSL_GUARDED_BY(program_name_guard) = nullptr;

std::string ProgramInvocationName() {
	absl::MutexLock l(&program_name_guard);

	return program_name ? *program_name : "UNKNOWN";
}

std::string ShortProgramInvocationName() {
	absl::MutexLock l(&program_name_guard);

	return program_name ? std::string(flags_internal::Basename(*program_name))
	       : "UNKNOWN";
}

void SetProgramInvocationName(absl::string_view prog_name_str) {
	absl::MutexLock l(&program_name_guard);

	if(!program_name)
		program_name = new std::string(prog_name_str);
	else
		program_name->assign(prog_name_str.data(), prog_name_str.size());
}
}  // namespace flags_internal
ABSL_NAMESPACE_END
}  // namespace absl
