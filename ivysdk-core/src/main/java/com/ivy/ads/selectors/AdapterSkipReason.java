package com.ivy.ads.selectors;

public interface AdapterSkipReason {
  String DEBUG_FAIL = "debug";
  String SKIP_LOAD_FAILED_MANYTIMES = "skip_load_failed_manytimes";
  String SKIP_LOAD_TIMEOUT_MANYTIMES = "skip_load_timeout_manytimes";
  String SKIP_SHOW_FAILE_MANYTIMES = "skip_show_fail_manytimes";
  String SKIP_ZERO_WEIGHT = "skip_zero_weight";
  String SKIP_BY_COUNTRY = "skip_by_country";
  String SKIP_SLEEPING= "skip_sleeping";

  String SKIP_ZERO_DISPLAY = "skip_no_display";

  String NO_PLACEMENT_FAIL = "placement_missing";
}
