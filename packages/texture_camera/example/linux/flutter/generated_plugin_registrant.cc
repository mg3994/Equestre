//
//  Generated file. Do not edit.
//

// clang-format off

#include "generated_plugin_registrant.h"

#include <texture_camera/texture_camera_plugin.h>

void fl_register_plugins(FlPluginRegistry* registry) {
  g_autoptr(FlPluginRegistrar) texture_camera_registrar =
      fl_plugin_registry_get_registrar_for_plugin(registry, "TextureCameraPlugin");
  texture_camera_plugin_register_with_registrar(texture_camera_registrar);
}
