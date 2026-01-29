import { Button } from "@/components/ui/button"
import Layout from "../components/Layout"
import { Dialog, DialogContent, DialogFooter, DialogHeader } from "../components/ui/dialog"
import { DialogClose, DialogDescription, DialogTitle, DialogTrigger } from "@radix-ui/react-dialog"
import { useState, type ComponentProps } from "react"
import { ToggleGroup, ToggleGroupItem } from "../components/ui/toggle-group"
import { useNavigate } from "react-router"

function App() {
  return (
    <Layout>
      <div className="flex flex-col gap-4 items-center">
        <PlayHumanButton className='min-w-3xs' />
        <PlayComputerButton className='min-w-3xs' />
      </div>
    </Layout>)
}

/**
 * For now there aren't any options to customise, so we just give the two buttons of "join queue" or "cancel" in this dialog
 */
const PlayHumanButton = (props: ComponentProps<typeof Button>) => {
  const titleText = "Play against humans";
  const descriptionText = "Get matched with other players";
  const navigate = useNavigate();
  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button {...props}>{titleText}</Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="font-bold">{titleText}</DialogTitle>
          <DialogDescription className="text-sm">{descriptionText}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant='outline'>Cancel</Button>
          </DialogClose>
          <Button onClick={() => navigate('/test')}>Join queue</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

const PlayComputerButton = (props: ComponentProps<typeof Button>) => {
  const titleText = "Play against a computer";
  const descriptionText = "Play against a computer opponent as the colour of your choice";
  const [colour, setColour] = useState('Random');
  const navigate = useNavigate();
  return (
    <Dialog>
      <form>
        <DialogTrigger asChild>
          <Button {...props}>{titleText}</Button>
        </DialogTrigger>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="font-bold">{titleText}</DialogTitle>
            <DialogDescription className="text-sm">{descriptionText}</DialogDescription>
          </DialogHeader>

          <div className='flex items-center gap-3'>
            <p>Play as:</p>
            <ToggleGroup type='single' variant='outline' value={colour} onValueChange={setColour}>
              <ToggleGroupItem value='White'>
                White
              </ToggleGroupItem>
              <ToggleGroupItem value='Black'>
                Black
              </ToggleGroupItem>
              <ToggleGroupItem value='Random'>
                Random
              </ToggleGroupItem>
            </ToggleGroup>
          </div>

          <DialogFooter>
            <DialogClose asChild>
              <Button variant='outline'>Cancel</Button>
            </DialogClose>
            <Button onClick={() => navigate(`/play/${colour}`)}>Play</Button>
          </DialogFooter>
        </DialogContent>
      </form>
    </Dialog>
  )
}

export default App
