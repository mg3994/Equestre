#include "include/texture_camera/texture_camera_plugin_c_api.h"

#include <flutter/plugin_registrar_windows.h>

#include "texture_camera_plugin.h"

void TextureCameraPluginCApiRegisterWithRegistrar(
    FlutterDesktopPluginRegistrarRef registrar) {
  texture_camera::TextureCameraPlugin::RegisterWithRegistrar(
      flutter::PluginRegistrarManager::GetInstance()
          ->GetRegistrar<flutter::PluginRegistrarWindows>(registrar));
}
