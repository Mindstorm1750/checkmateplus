import Layout from "@/components/Layout";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { usePreferences, type Preferences } from "@/lib/PreferencesContext"

function Settings() {
    const { preferences, updatePreferences } = usePreferences();

    return (
        <Layout>
            <p>Board colour theme:</p>
            <ToggleGroup type='single' variant='outline' value={preferences.boardColor} onValueChange={(e) => updatePreferences({boardColor: e as Preferences['boardColor']})}>
              <ToggleGroupItem value='BROWN'>
                Brown
              </ToggleGroupItem>
              <ToggleGroupItem value='BLUE'>
                Blue
              </ToggleGroupItem>
              <ToggleGroupItem value='GREEN'>
                Green
              </ToggleGroupItem>
            </ToggleGroup>

            <p>Piece animations:</p>
            <ToggleGroup type='single' variant='outline' value={preferences.animationType} onValueChange={(e) => updatePreferences({animationType: e as Preferences['animationType']})}>
              <ToggleGroupItem value='NORMAL'>
                Normal
              </ToggleGroupItem>
              <ToggleGroupItem value='FUN'>
                Fun
              </ToggleGroupItem>
            </ToggleGroup>
        </Layout>
    )
}

export default Settings