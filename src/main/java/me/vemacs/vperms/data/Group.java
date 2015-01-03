package me.vemacs.vperms.data;

import lombok.Data;
import lombok.NonNull;
import me.vemacs.vperms.storage.GroupDataSource;
import me.vemacs.vperms.vPermsPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class Group {
    @NonNull
    private String name;
    @NonNull
    private List<String> parents;
    @NonNull
    private Map<String, Boolean> permissions;

    public String getName() {
        return name.toLowerCase();
    }

    public Map<String, Boolean> computeEffectivePermissions() {
        Map<String, Boolean> perms = new LinkedHashMap<>();
        for (String ancestor : calculateGroupTree())
            perms.putAll(getGroupFor(ancestor).getPermissions());
        return perms;
    }

    public static <T> List<T> squash(List<T> toSquash) {
        List<T> tmp = new ArrayList<>();
        for (T element : toSquash)
            if (!tmp.contains(element)) tmp.add(element);
        return tmp;
    }

    public List<String> calculateGroupTree() {
        List<String> tree = new ArrayList<String>();
        tree.add(0, getName());
        for (String top : parents) {
            if (top.equalsIgnoreCase(getName())) {
                continue;
            }
            for (String trunk : calculateBackwardTree(top)) {
                tree.add(0, trunk);
            }
        }
        return squash(tree);
    }

    private List<String> calculateBackwardTree(String group) {
        List<String> tree = new ArrayList<String>();
        tree.add(group);
        for (String top : getGroupFor(group).getParents()) {
            if (top.equalsIgnoreCase(group)) {
                continue;
            }
            if (getGroupFor(top).getParents().contains(group)) {
                String errorMessage = "Group " + getName() + " has a circular inheritance issue.";
                try {
                    vPermsPlugin.getInstance().getLogger().warning(errorMessage);
                } catch (NullPointerException e) {
                    System.out.println("[WARNING] " + errorMessage);
                }
                continue;
            }
            for (String trunk : calculateBackwardTree(top)) {
                tree.add(trunk);
            }
        }
        return tree;
    }

    public static Group getGroupFor(String name) {
        return vPermsPlugin.getDataSource().getGroup(name);
    }

    @SuppressWarnings("unchecked")
    public String serializedForm() {
        JSONObject json = new JSONObject();
        json.put("name", getName());
        json.put("parents", getParents());
        json.put("permissions", new JSONObject(getPermissions()));
        return json.toJSONString();
    }
}

