package net.melbourne.modules;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import lombok.Getter;
import net.melbourne.Manager;
import net.melbourne.Melbourne;
import net.melbourne.settings.Setting;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class FeatureManager extends Manager {
    private static final Reflections REFLECTIONS = new Reflections("net.melbourne.modules.impl");
    private final List<Feature> features = new ArrayList<>();
    private final Map<Class<? extends Feature>, Feature> featuresClasses = new Reference2ReferenceOpenHashMap<>();

    public FeatureManager() {
        super("Feature", "Allows you to register, or access every feature in " + Melbourne.NAME);
    }

    @Override
    public void onInit() {
        Set<Class<? extends Feature>> set = REFLECTIONS.getSubTypesOf(Feature.class);

        for (Class<? extends Feature> clazz : set) {
            try {
                if (clazz.getAnnotation(FeatureInfo.class) == null)
                    continue;

                Feature feature = clazz.getDeclaredConstructor().newInstance();

                for (Field field : feature.getClass().getDeclaredFields()) {
                    if (!Setting.class.isAssignableFrom(field.getType()))
                        continue;

                    if (!field.canAccess(feature)) field.setAccessible(true);

                    feature.getSettings().add((Setting) field.get(feature));
                }

                Collections.addAll(feature.getSettings(), feature.bind, feature.bindMode, feature.hiddenMode);
                this.features.add(feature);
                featuresClasses.put(feature.getClass(), feature);

            } catch (Exception e) {
                Melbourne.getLogger().error("Error while instantiating {}", clazz.getName(), e);
            }
        }

        features.sort(Comparator.comparing(Feature::getName));
    }

    @SuppressWarnings("unchecked")
    public <T extends Feature> T getFeatureFromClass(Class<T> clazz) {
        return (T) featuresClasses.get(clazz);
    }

    public Feature getFeatureByName(String name) {
        for (Feature feature : features) {
            if (feature.getName().equalsIgnoreCase(name)) return feature;
        }
        return null;
    }

    public List<Feature> getFeaturesInCategory(Category c) {
        return this.features.stream().filter(m -> m.getCategory() == c).toList();
    }
}