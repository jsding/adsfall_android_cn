package com.sherdle.universal.inherit;

/**
 * This is an interface for fragments to easily request permissions
 */
public interface PermissionsFragment {

  /**
   * A string array of all the permissions this Fragment requires.
   *
   * @return String array of permissions
   */
  String[] requiredPermissions();

}
