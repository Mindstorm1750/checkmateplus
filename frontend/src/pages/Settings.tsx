import Layout from "@/components/Layout";
import { BackArrowLink } from "@/components/ui/back-arrow-link";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { usePreferences, type Preferences } from "@/lib/PreferencesProvider"

function Settings() {
  const { preferences, updatePreferences } = usePreferences();

  return (
    <Layout>
      <div className='flex flex-col gap-2'>
        <p>Board colour theme:</p>
        <ToggleGroup type='single' variant='outline' value={preferences.boardColour} onValueChange={(e) => updatePreferences({ boardColour: e as Preferences['boardColour'] })}>
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
        <ToggleGroup type='single' variant='outline' value={preferences.animationType} onValueChange={(e) => updatePreferences({ animationType: e as Preferences['animationType'] })}>
          <ToggleGroupItem value='NORMAL'>
            Normal
          </ToggleGroupItem>
          <ToggleGroupItem value='FUN'>
            Fun
          </ToggleGroupItem>
        </ToggleGroup>
        <BackArrowLink />
      </div>
    </Layout>
  )
}

export default Settings