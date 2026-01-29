package net.melbourne.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.melbourne.events.Event;
import net.melbourne.settings.Setting;


@Getter
@AllArgsConstructor
public class SettingChangeEvent extends Event
{
	private final Setting setting;
}
