import sys
import pickle

def create_problem(scenario):
    init = {}
    goal = {}
    actions = {}

    # Construiesc starea initiala.
    init['Carries'] = -1
    init['Position'] = scenario['initial_position']
    goal['hasProduct'] = []

    # Construiesc goal-ul.
    goal['Carries'] = -1
    goal['hasProduct'] = []
    for order in scenario['oreders']:
        goal['hasProduct'].append(order)

    # Construiesc actiunile.
    init_pos_is_flyable = True
    flyable_cells = scenario['warehouses'] + scenario['clients']
    
    if scenario['initial_position'] not in flyable_cells:
        init_pos_is_flyable = False
        flyable_cells.append(scenario['initial_position'])
    
    actions['Fly'] = {}
    for i in flyable_cells:
        for j in flyable_cells:
            if i == j:
                continue
            if not init_pos_is_flyable:
                if j == scenario['initial_position']:
                    continue
            actions['Fly'][(i, j)] = ({'Position': i}, {'Position': j})

    actions['Load'] = {}
    for w in scenario['warehouses']:
        for p in range(scenario['number_of_products']):
            if (w, p) in scenario['available_products']:
                actions['Load'][(w, p)] = ({
                    'Position': w,
                    'Warehouse': w,
                    'hasProudct': (w, p),
                    'Carries': -1
                }, {
                    'Carries': p
                })

    actions['Deliver'] = {}
    for c in scenario['clients']:
        for p in range(scenario['number_of_products']):
            if (c, p) in scenario['orders']:
                actions['Deliver'][(c, p)] = ({
                    'Position': c,
                    'Client': c,
                    'Order': (c, p),
                    'Carries': p
                },{
                    'Carries': -1,
                    'hasProduct': (c, p)
                })
    
    return init, goal, actions


def 

def make_plan(scenario, state = [], goals = [], depth = 0):
    if (len(state) == 0):
        state, goals, scenario['actions'] = create_problem(scenario)
    
    if depth > 10:
        return None

    i = 0
    while i < len(goals):
        goal = goals[i]

        if 

    

    return False

def main(args):
    scenario = pickle.load(open('example.pkl'))

    plan = make_plan(scenario)

    print(plan)

if __name__ == '__main__':
    main(sys.argv)
