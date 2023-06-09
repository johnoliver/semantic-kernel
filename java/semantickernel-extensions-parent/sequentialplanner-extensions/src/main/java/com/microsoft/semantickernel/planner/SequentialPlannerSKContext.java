// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.planner;

import com.microsoft.semantickernel.orchestration.SKContext;
import com.microsoft.semantickernel.textcompletion.CompletionSKContext;
import javax.annotation.Nullable;
import reactor.core.publisher.Mono;

public interface SequentialPlannerSKContext extends SKContext<CompletionSKContext> {

  public static final String PlannerMemoryCollectionName = "Planning.SKFunctionsManual";

  public static final String PlanSKFunctionsAreRemembered = "Planning.SKFunctionsAreRemembered";

  public Mono<String> getFunctionsManualAsync(
      @Nullable String semanticQuery, @Nullable SequentialPlannerRequestSettings config);
}
